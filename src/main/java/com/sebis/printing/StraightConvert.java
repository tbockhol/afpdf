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

import java.io.IOException;
import java.io.FileNotFoundException;

import com.mgz.afp.enums.AFPColorValue;
import com.mgz.afp.exceptions.AFPParserException;
import com.mgz.afp.foca.EFN_EndFont;
import com.mgz.afp.ioca.ImageObject;
import com.mgz.afp.modca.BPG_BeginPage;
import com.mgz.afp.modca.EIM_EndImageObject;
import com.mgz.afp.modca.EPG_EndPage;
import com.mgz.afp.modca.MCF_MapCodedFont_Format2;
import com.mgz.afp.ptoca.PTX_PresentationTextData;
import com.mgz.afp.ptoca.controlSequence.PTOCAControlSequence;
import com.mgz.afp.ptoca.controlSequence.PTOCAControlSequence.*;

/**
 * just do a straight afp to pdf conversion 
 */
public class StraightConvert extends BaseParser {

	OutputDocument pdfOut;

	int fontIdx = 1;
	int textTop = 0;

	int[] rgb = new int[] {0,0,0};

	public StraightConvert() {}
	public StraightConvert(String inputFilename) throws IOException {
		super(inputFilename);
	}
	public void setInputParser(String inputFilename) throws IOException {
		super.setInputParser(inputFilename);
	}
	public void setOutputDocument(String outputFilename) throws FileNotFoundException {
		this.pdfOut = new OutputDocument(outputFilename);
	}
	// close input and output
	public void closeAll() throws IOException {
		super.close();
		this.pdfOut.close();
	}
	
	@Override
	public void handleBPG_BeginPage(BPG_BeginPage sf) {
		super.handleBPG_BeginPage(sf);
		//this.pdfOut.addPage();
	}
	@Override
	public void handleEPG_EndPage(EPG_EndPage sf) {
		super.handleEPG_EndPage(sf);
		this.pdfOut.addPageBreak();
	}
	@Override
	public void handleEFN_EndFont(EFN_EndFont sf) {
		super.handleEFN_EndFont(sf);
	}

	@Override
	public void handleMCF_MapCodedFont_Format2(MCF_MapCodedFont_Format2 sf) {
		super.handleMCF_MapCodedFont_Format2(sf);

		// add all fonts after they are mapped
		for (FontProperties font : super.fonts){ 
			this.pdfOut.addRasterFont(font);
		}
	}
	@Override
	public void handlePTX_PresentationTextData(PTX_PresentationTextData sf) {
		super.handlePTX_PresentationTextData(sf);

		textLeft = 0;
		textData = "";

		for (PTOCAControlSequence s: sf.getControlSequences()) {
			//System.out.println(s);

			if (s instanceof NOP_NoOperation) {
				NOP_NoOperation nop = (NOP_NoOperation)s;
				String text = new String(nop.getIgnoredData(), super.config.getAfpCharSet());
				//System.out.println(text);
			}
			
			if (s instanceof AMB_AbsoluteMoveBaseline) {
				AMB_AbsoluteMoveBaseline amb = (AMB_AbsoluteMoveBaseline)s;
				textTop = amb.getDisplacement() + super.getPageyOffset();
			}
			if (s instanceof AMI_AbsoluteMoveInline) {
				AMI_AbsoluteMoveInline ami = (AMI_AbsoluteMoveInline)s;
				textLeft = ami.getDisplacement() + super.getPagexOffset();
			}
			if (s instanceof SCFL_SetCodedFontLocal) {
				SCFL_SetCodedFontLocal scfl = (SCFL_SetCodedFontLocal)s;

				fontIdx = scfl.getCodedFontLocalID();
			}

			if (s instanceof STC_SetTextColor) {
				STC_SetTextColor stc = (STC_SetTextColor)s;
				AFPColorValue cv = stc.getForegroundColor();
				rgb = cv.toRgb();
			}
			
			if (s instanceof DIR_DrawIaxisRule) {
				DIR_DrawIaxisRule dir = (DIR_DrawIaxisRule)s;
				// input units 1/1440", output 1/72"
				float top = textTop / 20;
				float left = textLeft/ 20;
				float length = dir.getLength() / 20;
				float width = dir.getWidth() / 20;

				this.pdfOut.drawRule(left, top, left + length, top, width);
			}
			if (s instanceof DBR_DrawBaxisRule) {
				DBR_DrawBaxisRule dir = (DBR_DrawBaxisRule)s;
				// input units 1/1440", output 1/72"
				float top = textTop / 20;
				float left = textLeft/ 20;
				float length = dir.getLength() / 20;
				float width = dir.getWidth() / 20f;
				
				this.pdfOut.drawRule(left, top, left, top + length, width);
			}

			if (s instanceof TRN_TransparentData) {
				TRN_TransparentData td = (TRN_TransparentData)s;
				textData = td.getTransparentData();

				// input units 1/1440", output 1/72"
				float top = textTop / 20;
				float left = textLeft / 20;
				
				// to measure from bottom, need to shift line down by max descender, 1/1000 to 1/72
				FontProperties font = this.fonts.get(fontIdx-1);
				float fontShift = (font.getMaxDescender()/500f) * (font.getNominalSize()) ;
			
				this.pdfOut.addText(textData, fontIdx, fontShift, left, top, rgb);

				//System.out.println(String.format("%d (%d,%d) %s", fontIdx, textLeft, textTop, textData));
			}
		}
	}
	
	protected void handleEIM_EndImageObject(EIM_EndImageObject sf) {
		super.handleEIM_EndImageObject(sf);
		ImageObject img = super.config.getCurrentImageObject();
		
		// input units 1/1440", output 1/72"
		float x = (img.getXOrigin() + super.getPagexOffset()) / 20;
		float y = (img.getYOrigin() + super.getPageyOffset()) / 20;
		this.pdfOut.addRawImage(BaseParser.flipBits(img.getImageData()), img.getWidth(), img.getHeight(), x, y);
	}

	public static void main(String[] argv) {
		
		if (argv.length != 2) {
			System.err.println("Provide input and output files.");
			System.exit(5);
		}
		
		String inputFilename = argv[0];
		String outputFilename = argv[1];
		StraightConvert convert = new StraightConvert();
		
		try {
			convert.setInputParser(inputFilename);
		} catch(IOException e) {
			System.err.println("Problem with input file " + inputFilename);
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			convert.setOutputDocument(outputFilename);
		} catch (FileNotFoundException e) {
			System.err.println("Problem with output file " + outputFilename);
			e.printStackTrace();
			System.exit(2);
		}
		
		try {
			convert.parse();
		} catch (AFPParserException e) {
			System.err.println("Parser exception");
			e.printStackTrace();
			System.exit(3);
		}
		
		try {
			convert.closeAll();
		} catch (IOException e) {
			System.err.println("Problem closing (input)");
			e.printStackTrace();
			System.exit(4);
		}
	}
}
