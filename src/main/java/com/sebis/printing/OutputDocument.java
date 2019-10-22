/*  This file is a part of AFPDF - AFP/PDF transformer
Copyright (C) 2019  Sebis Direct, Inc.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.sebis.printing;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.image.RawImageData;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfType3Font;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.AreaBreakType;


/**
 * itext implementation to build pdf documents
 */
public class OutputDocument {
	
	private String outputFilename;
	private PdfDocument pdfDoc;
	private Document doc;
	
	private List<PdfType3Font> fonts;
	
	public OutputDocument (String outputFilename) throws FileNotFoundException {
		this.outputFilename = outputFilename;
		this.pdfDoc = new PdfDocument(new PdfWriter(this.outputFilename));
		this.pdfDoc.setDefaultPageSize(PageSize.LETTER);
		this.doc = new Document(pdfDoc);
		this.fonts = new ArrayList<PdfType3Font>();
	}
	
	/*
	 * add 1-bit image from raw data
	 */
	public void addRawImage(byte[] data, int width, int height, float left, float top) {
		// er 8.5x11" for now, image is 300dpi
		float bottom = 792 - top - (height/4.1666f);

		RawImageData imageData =  (RawImageData) ImageDataFactory.create(width, height, 1, 1, data, null);
		Image image = new Image(imageData, left, bottom, (width/4.1666f));
		
		//Image image2 = image.setBackgroundColor(new DeviceRgb(255, 0, 0));
		
		this.doc.add(image);
	}
	
	/*
	 * add raster style font
	 */
	public int addRasterFont(FontProperties font) {
		PdfType3Font t3 = PdfFontFactory.createType3Font(this.pdfDoc, false);
		
		for (String key : font.getGlyphs().keySet()) {
		
			GlyphProperties g = font.getGlyph(key);

			// canvas is 6000 dpi?
			float tx_factor = (font.getNominalSize()/72f) * 6000;
			int cell_y = (int)(tx_factor);

			// fonts are base 1000
			int width = (int) (g.getAdvance() * (tx_factor/1000f));
			
			PdfCanvas i = t3.addGlyph((char)g.getCodePoint(), width, 0, 0, width, cell_y);
			
			//i.rectangle(0,0,width,cell_y).stroke();
		
			RawImageData data =  (RawImageData) ImageDataFactory.create(g.getCharWidth(),g.getCellHeight(), 1, 1, g.data, null);
		
			data.makeMask();
			
			float scale_x = (g.getBspace()) * (tx_factor / 1000f);
			float scale_y = g.getCharacterHeight() * (tx_factor / 1000f);

			
			float shift_y=0;
			
			int baseline = font.getMaxDescender();
			
			// ignore negative descender?
			int glyphDescender = 0;
			if (g.getDescender() > 0) {
				glyphDescender = g.getDescender();
			}
			
			shift_y = (baseline - glyphDescender) * (tx_factor / 1000f);
			
			if (g.getBaselineShift() > g.getCharacterHeight()) {
				shift_y += (g.getBaselineShift()- g.getCharacterHeight());
			}
			
			int shift_x = (int)(g.getAspace() * (tx_factor / 1000f));

			i.addImage(data,scale_x,0,0,scale_y,shift_x,shift_y,true);
		}
		
		this.fonts.add(t3);
		return this.fonts.size();
	}
	
	/*
	 * add text in font at position
	 */
	public void addText(String text, int font, float fontShift, float left, float top, int[] rgb) {
		
		//System.out.println(String.format("add text (font %d, shift %f): %s\n", font-1, fontShift, text));
		
		// er 8.5x11" for now
		float bottom = 792 - top;
		// all boxes go to end of page
		float width = 612 - left;
		
		//Source measures from top, subtract line height
		bottom -= fontShift;
		
		
		DeviceRgb drgb = new DeviceRgb(rgb[0], rgb[1], rgb[2]);
		
		// prepend null makes itext preserve leading spaces
		Paragraph p = new Paragraph().add("\u0000").add(text)
				.setFixedPosition(left,bottom,width).setFontColor(drgb).setFont(fonts.get(font-1));

		this.doc.add(p);
	}

	/*
	 *  draw a line
	 */
	public void drawRule(float l1, float t1, float l2, float t2, float width) {

		// er 8.5x11" for now
		float b1 = 792 - t1;
		float b2 = 792 - t2;
		
		PdfCanvas canvas = new PdfCanvas(this.pdfDoc.getLastPage());
		
		canvas.setStrokeColor(new DeviceRgb(0,0,0)).setLineWidth(width).moveTo(l1,  b1).lineTo(l2, b2).stroke();
	}
	
	public void finalize() {
		this.close();
	}
	public void close() {
		this.doc.close();
	}

	public void addPage() {
		//this.currentCanvas = new PdfCanvas(this.pdfDoc.addNewPage());
	}
	
	public void addPageBreak() {
		this.doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
	}

}
