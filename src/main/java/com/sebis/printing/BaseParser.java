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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mgz.afp.base.IRepeatingGroup;
import com.mgz.afp.base.StructuredField;
import com.mgz.afp.enums.AFPUnitBase;
import com.mgz.afp.exceptions.AFPParserException;
import com.mgz.afp.foca.*;
import com.mgz.afp.foca.CPI_CodePageIndex.CPI_RepeatingGroup;
import com.mgz.afp.foca.FNC_FontControl.PatternTechnologyIdentifier;
import com.mgz.afp.foca.FNC_FontControl.RasterPatternDataAlignment;
import com.mgz.afp.foca.FNM_FontPatternsMap.FNM_RepeatingGroup;
import com.mgz.afp.foca.FNP_FontPosition.FNP_RepeatingGroup;
import com.mgz.afp.ioca.*;
import com.mgz.afp.ioca.IDD_SelfDefiningField.SetBilevelImageColor;
import com.mgz.afp.parser.AFPParser;
import com.mgz.afp.parser.AFPParserConfiguration;
import com.mgz.afp.ptoca.*;
import com.mgz.afp.triplets.Triplet;
import com.mgz.afp.triplets.Triplet.GlobalID_Use;
import com.mgz.afp.modca.*;
import com.mgz.afp.modca.MCF_MapCodedFont_Format2.MCF_RepeatingGroup;

/**
 * Base parser class. Maintains state of AFP stream.
 * extend the hooks to implement business logic
 */
public class BaseParser {
	
	protected String inputFilename;
	protected AFPParser parser;
	protected AFPParserConfiguration config;

	Map<String, FNI_FontIndex.FNI_RepeatingGroup> fontIndexes = new HashMap<String,  FNI_FontIndex.FNI_RepeatingGroup>();
	List<FNM_RepeatingGroup> fontPatternsMap = new ArrayList<FNM_RepeatingGroup>();
	byte[] currentFontPatterns = new byte[0];
	
	String currentCodePage;
	Map<String, CodePage> codePages = new HashMap<String, CodePage>();
	FontProperties currentFont;
	List<FontProperties> fonts = new ArrayList<FontProperties>();
	
	Medium currentMedium;
	Map<String, Medium> media = new HashMap<String,Medium>();
	
	// current page coordinate origin offset set on medium map
	protected int pagexOffset;
	protected int pageyOffset;
	
	boolean fontsMapped = false; //only match glyphs to code points once, not on every page.
	int fontCoded = 0; // font being coded, index
	
	// current presentation text settings
	int textTop = 0;
	int textLeft = 0;
	String textData = "";
	
	public BaseParser() {}
	public BaseParser(String inputFilename) throws IOException {
		this.setInputParser(inputFilename);
	}
	public void setInputParser(String inputFilename) throws IOException {
		this.config = new AFPParserConfiguration();
		this.config.setInputStream(new FileInputStream(inputFilename));
		this.parser = new AFPParser(config);
	}
	
	public void close() throws IOException {
		this.config.getInputStream().close();
	}
	
	protected void handleBMM_BeginMediumMap(BMM_BeginMediumMap sf) {
		this.currentMedium = new Medium(sf.getName());
	}
	protected void handleMDD_MediumDescriptor(MDD_MediumDescriptor sf) {
		if (sf.getxUnitBase() != AFPUnitBase.Inches10 || sf.getxUnitsPerUnitBase() != 14400) {
			System.err.println("Only 1440 per Inch medium supported.");
		}
	}
	protected void handleEMM_EndMediumMap(EMM_EndMediumMap sf) {
		media.put(currentMedium.getName(), currentMedium);
	}
	protected void handleIMM_InvokeMediumMap(IMM_InvokeMediumMap sf) {
		Medium m = this.media.get(sf.getName());
		this.pagexOffset = m.getxOrigin();
		this.pageyOffset = m.getyOrigin();
	}
	protected void handleBCP_BeginCodePage(BCP_BeginCodePage sf) {
		this.currentCodePage = sf.getName();
		this.codePages.put(sf.getName(), new CodePage(sf.getName()));
	}
	protected void handleCPC_CodePageControl(CPC_CodePageControl sf) {}
	protected void handleCPD_CodePageDescriptor(CPD_CodePageDescriptor sf) {
		//System.out.println(sf.getGraphicCharacterSetGID());
		//System.out.println(sf.getCodePageGID());
	}
	protected void handleCPI_CodePageIndex(CPI_CodePageIndex sf) {
		for (CPI_RepeatingGroup g : sf.getRepeatingGroups()) {
			this.codePages.get(currentCodePage).addCodePoint(g.getGraphicCharacterGID(), g.getCodePoint());
		}
	}
	protected void handleECP_EndCodePage(ECP_EndCodePage sf) {
		//System.out.println(currentCodePage);
		//System.out.println(codePages.get(currentCodePage).getCodePoints());
	}
	protected void handleBFN_BeginFont(BFN_BeginFont sf) {
		this.fontIndexes.clear();
		this.fontPatternsMap.clear();
		this.currentFontPatterns = new byte[0];
		this.currentFont = new FontProperties();
	}
	protected void handleBPG_BeginPage(BPG_BeginPage sf) {}
	protected void handleEPG_EndPage(EPG_EndPage sf) {}
		
	protected void handleFNC_FontControl(FNC_FontControl sf) {
		if (!sf.getPatternTechnologyIdentifier().equals(PatternTechnologyIdentifier.LaserMatrixNBitWide)) {
			System.err.println("Only LaserMatrixNBitWide fonts supported.");
		}
		if (!sf.getRasterPatternDataAlignment().equals(RasterPatternDataAlignment.Alignment_1Byte)) {
			System.err.println("Only 1 byte raster pattern alignment supported.");
		}
		/* only 300 dpi fixed base
		System.out.println(sf.getxUnitBase());
		System.out.println(sf.getxUnitsPerUnitBase());
		System.out.println(sf.getyUnitBase());
		System.out.println(sf.getyUnitsPerUnitBase());
		System.out.println(sf.getShapeResolutionXUnitsPerUnitBase());
		System.out.println(sf.getShapeResolutionYUnitsPerUnitBase());
		System.out.println(sf.getShapeResolutionXUnitBase10Inches());
		System.out.println(sf.getShapeResolutionYUnitBase10Inches());
		System.out.println(sf.getMaxCharacterBoxHeight() + "x" + sf.getMaxCharacterBoxWidth());
		 */
		this.currentFont.setMaxCharacterHeight(sf.getMaxCharacterBoxHeight());
		this.currentFont.setMaxCharacterWidth(sf.getMaxCharacterBoxWidth());
	}
	protected void handleFND_FontDescriptor(FND_FontDescriptor sf) {
		this.currentFont.setNominalSize(sf.getNominalVerticalSize()/10);
		//System.out.println("VER:" + sf.getNominalVerticalSize() + " " + sf.getMaxVerticalSize());
	}
	protected void handleFNO_FontOrientation(FNO_FontOrientation sf) {
		/*
		for (FNO_RepeatingGroup g : sf.getRepeatingGroups()) {
			System.out.println(g.getCharacterRotation());
			System.out.println(g.getEmSpaceIncrement());
			System.out.println(g.getDefaultBaselineIncrement());
		}*/
	}
	protected void handleFNP_FontPosition(FNP_FontPosition sf) {
		FNP_RepeatingGroup g = sf.getRepeatingGroups().get(0);
		this.currentFont.setMaxAscender(g.getMaxAscenderHeight());
		this.currentFont.setMaxDescender(g.getMaxDescenderDepth());
	}
	
	protected void handleFNI_FontIndex(FNI_FontIndex sf) {
		//  only get the first (0 degrees rotation)
		if (this.fontIndexes.size() == 0) {
			for (FNI_FontIndex.FNI_RepeatingGroup g : sf.getRepeatingGroups()) {
				this.fontIndexes.put(g.getGraphicCharacterGlobalID_GCGID(), g);
			}
		}
	}
	protected void handleFNM_FontPatternsMap(FNM_FontPatternsMap sf) {
		for (FNM_RepeatingGroup g : sf.getRepeatingGroups()) {
			this.fontPatternsMap.add(g);
			//System.out.println(g.getPatternDataOffset());
			//System.out.println(g.getCharacterBoxWidth() + "x" + g.getCharacterBoxHeight());
			
		}
	}
	protected void handleFNG_FontPatterns(FNG_FontPatterns sf) {
		byte[] buff = new byte[ currentFontPatterns.length + sf.getData().length ];
		System.arraycopy(currentFontPatterns, 0, buff, 0, currentFontPatterns.length);
		System.arraycopy(sf.getData(), 0, buff, currentFontPatterns.length, sf.getData().length);
		this.currentFontPatterns = buff;
	}
	protected void handleBIM_BeginImageObject(BIM_BeginImageObject sf) {}
	protected void handleEIM_EndImageObject(EIM_EndImageObject sf) {
		//this.config.getCurrentImageObject();
	}
	protected void handleOBP_ObjectAreaPosition(OBP_ObjectAreaPosition sf) {
		OBP_ObjectAreaPosition.OBP_RepeatingGroup g = sf.getRepeatingGroup();
		if (this.config.getCurrentImageObject() != null) {
			this.config.getCurrentImageObject().setOffset(g.getxOrigin(),g.getyOrigin());
		}
	}
	protected void handleOBD_ObjectAreaDescriptor(OBD_ObjectAreaDescriptor sf) {
		/*
		for (Triplet t : sf.getTriplets()) {
			System.out.println(t.toString());
		}*/
	}
	protected void handleIDD_ImageDataDescriptor(IDD_ImageDataDescriptor sf) {
		for (IDD_SelfDefiningField g : sf.getSelfDefiningFields()) {
			if (g instanceof SetBilevelImageColor) {
				SetBilevelImageColor bic = (SetBilevelImageColor) g;
				if (this.config.getCurrentImageObject() != null) {
					this.config.getCurrentImageObject().setBilevelColor(bic.getColor());
				}
			}
		}
	}
	protected void handleIPD_ImagePictureData(IPD_ImagePictureData sf) {}
	protected void handlePGD_PageDescriptor(PGD_PageDescriptor sf) {
		//System.out.println(sf.getxSize());
		//System.out.println(sf.getySize());
	}
	protected void handlePGP_PagePosition_Format1(PGP_PagePosition_Format1 sf) {
		this.currentMedium.setxOrigin(sf.getxOrigin());
		this.currentMedium.setyOrigin(sf.getxOrigin());
	}
	protected void handleBPT_BeginPresentationTextObject(BPT_BeginPresentationTextObject sf) {}
	protected void handlePTX_PresentationTextData(PTX_PresentationTextData sf) {}

	protected void handlePTD_PresentationTextDataDescriptor_Format2(PTD_PresentationTextDataDescriptor_Format2 sf) {
	}

	protected void handleEFN_EndFont(EFN_EndFont sf) {
		// turn afp data into list of glyph properties
		//System.out.println(this.currentCodePage);
		for (String gcgid: fontIndexes.keySet()) {

			FNI_FontIndex.FNI_RepeatingGroup metrics = fontIndexes.get(gcgid);
			FNM_RepeatingGroup pm = fontPatternsMap.get(metrics.getFnmIndex());

			// rows get padded to 8, index starts at 0 (add 1)
			int cellWidth = pm.getCharacterBoxWidth() + 1;
			int cellHeight = pm.getCharacterBoxHeight() + 1;
				
			if (cellWidth % 8 != 0) {
				cellWidth = cellWidth + (8 - (cellWidth%8));
			}
						
			int cellSize = (cellWidth * cellHeight) / 8;
					
			// parse out the relevant pattern
			byte[] buf = new byte[cellSize];
			System.arraycopy(currentFontPatterns, (int)pm.getPatternDataOffset(), buf, 0, cellSize);
			
			GlyphProperties glyph = new GlyphProperties(gcgid, flipBits(buf), cellWidth, cellHeight);
			glyph.setBaselineShift(metrics.getBaselineOffset());
			glyph.setAdvance(metrics.getCharacterIncrement());
			glyph.setAspace(metrics.getASpace());
			glyph.setBspace(metrics.getBSpace());
			glyph.setCspace(metrics.getCSpace());
			glyph.setAscender(metrics.getAscenderHeight());
			glyph.setDescender(metrics.getDescenderDepth());
			glyph.setCharWidth(pm.getCharacterBoxWidth() + 1);
			
			this.currentFont.addGlyph(gcgid, glyph);
		}
		
		this.fonts.add(this.currentFont);
		
	}

	protected void handleMCF_MapCodedFont_Format2(MCF_MapCodedFont_Format2 sf) {
		
		// MapCodedFont shows on each page, only doing this once for now.
		if (this.fontsMapped) {
			return;
		}
		fontsMapped = true;
	
		// look for code page name references, assign to fonts
		for (IRepeatingGroup g : sf.getRepeatingGroups()) {
			MCF_RepeatingGroup mcfg = (MCF_RepeatingGroup) g;

			for (Triplet t : mcfg.getTriplets()) {
				if (t instanceof Triplet.FullyQualifiedName) {
					if (((Triplet.FullyQualifiedName) t).getType() == GlobalID_Use.CodePageNameReference) {
						String codePageName = ((Triplet.FullyQualifiedName) t).getNameAsString();

						//System.out.println(codePageName);

						this.codeFonts(fontCoded, codePageName);
						fontCoded += 1;
					}
					if (((Triplet.FullyQualifiedName) t).getType() == GlobalID_Use.FontCharacterSetNameReference) {

						String charsetName = ((Triplet.FullyQualifiedName) t).getNameAsString();
						//System.out.println(charsetName);
					}

				}
			}
		}

		try {
			dumpFonts();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/*
	 * assign code points to font data
	 */
	private void codeFonts(int fontIdx, String codePageName) {
		
		FontProperties f = fonts.get(fontIdx);
		
		//System.out.println("font-" + fontIdx + " codepage " + codePageName );
		
		// Use default? codePage when not found
		if (!codePages.containsKey(codePageName)) {
			codePageName = "T1D0BASE";
		}

		
		for (String gcgid : f.glyphs.keySet()) {
			
			//System.out.println(codePageName);
			//System.out.println(codePages.get(codePageName));
			if (codePages.get(codePageName).getCodePoint(gcgid) == null) {
				//System.out.println(gcgid + " not found in codepage " + codePageName);
				continue;
			}

			// convert from ebcdic to ascii
			String c = new String(new byte[] {codePages.get(codePageName).getCodePoint(gcgid).byteValue()}, config.getAfpCharSet());
			
			f.glyphs.get(gcgid).setCodePoint(c.charAt(0));
			f.glyphs.get(gcgid).setCodePointEBCDIC(codePages.get(codePageName).getCodePoint(gcgid));
			
		}

		//setCodePoint(c.charAt(0));
		//glyph.setCodePointEBCDIC(currentCodePage.get(gcgid));
		//System.out.println(c.charAt(0) + " " + metrics.getBaselineOffset() + " " + metrics.getASpace() + " " + metrics.getBSpace() + " " + metrics.getCSpace());
	
	}


	public void parse () throws AFPParserException {
		
		StructuredField sf = null;
		
		do {
			
			sf = this.parser.parseNextSF();
			
			if (sf != null) {

				//System.out.println(sf.toString());

				if (sf instanceof BMM_BeginMediumMap) {
					handleBMM_BeginMediumMap((BMM_BeginMediumMap) sf);
				}
				if (sf instanceof MDD_MediumDescriptor) {
					handleMDD_MediumDescriptor((MDD_MediumDescriptor) sf);
				}
				if (sf instanceof EMM_EndMediumMap) {
					handleEMM_EndMediumMap((EMM_EndMediumMap) sf);
				}
				if (sf instanceof IMM_InvokeMediumMap) {
					handleIMM_InvokeMediumMap((IMM_InvokeMediumMap) sf);
				}
				if (sf instanceof BCP_BeginCodePage) {
					handleBCP_BeginCodePage((BCP_BeginCodePage) sf);
				}
				if (sf instanceof CPC_CodePageControl) {
					handleCPC_CodePageControl((CPC_CodePageControl) sf);
				}
				if (sf instanceof CPD_CodePageDescriptor) {
					handleCPD_CodePageDescriptor((CPD_CodePageDescriptor) sf);
				}
				if (sf instanceof CPI_CodePageIndex) {
					handleCPI_CodePageIndex((CPI_CodePageIndex) sf);
				}
				if (sf instanceof ECP_EndCodePage) {
					handleECP_EndCodePage((ECP_EndCodePage) sf);
				}
				if (sf instanceof BFN_BeginFont) {
					handleBFN_BeginFont((BFN_BeginFont) sf);
				}
				if (sf instanceof EFN_EndFont) {
					handleEFN_EndFont((EFN_EndFont) sf);
				}
				if (sf instanceof BPG_BeginPage) {
					handleBPG_BeginPage((BPG_BeginPage) sf);
				}
				if (sf instanceof EPG_EndPage) {
					handleEPG_EndPage((EPG_EndPage) sf);
				}
				if (sf instanceof FNC_FontControl) {
					handleFNC_FontControl((FNC_FontControl) sf);
				}
				if (sf instanceof FND_FontDescriptor) {
					handleFND_FontDescriptor((FND_FontDescriptor) sf);
				}
				if (sf instanceof FNI_FontIndex) {
					handleFNI_FontIndex((FNI_FontIndex) sf);
					
				}
				if (sf instanceof FNM_FontPatternsMap) {
					handleFNM_FontPatternsMap((FNM_FontPatternsMap) sf);
				}
				if (sf instanceof FNO_FontOrientation) {
					handleFNO_FontOrientation((FNO_FontOrientation) sf);
				}
				if (sf instanceof FNP_FontPosition) {
					handleFNP_FontPosition((FNP_FontPosition) sf);
				}
				if (sf instanceof FNG_FontPatterns) {
					handleFNG_FontPatterns((FNG_FontPatterns) sf);
				}
				if (sf instanceof MCF_MapCodedFont_Format2) {
					handleMCF_MapCodedFont_Format2((MCF_MapCodedFont_Format2) sf);
				}
				if (sf instanceof BIM_BeginImageObject) {
					handleBIM_BeginImageObject((BIM_BeginImageObject) sf);
				}
				if (sf instanceof EIM_EndImageObject) {
					handleEIM_EndImageObject((EIM_EndImageObject) sf);
				}
				if (sf instanceof IPD_ImagePictureData) {
					handleIPD_ImagePictureData((IPD_ImagePictureData) sf);
				}
				if (sf instanceof IDD_ImageDataDescriptor) {
					handleIDD_ImageDataDescriptor((IDD_ImageDataDescriptor) sf);
				}
				if (sf instanceof OBD_ObjectAreaDescriptor) {
					handleOBD_ObjectAreaDescriptor((OBD_ObjectAreaDescriptor) sf);
				}
				if (sf instanceof OBP_ObjectAreaPosition) {
					handleOBP_ObjectAreaPosition((OBP_ObjectAreaPosition) sf);
				}
				if (sf instanceof PGD_PageDescriptor) {
					handlePGD_PageDescriptor((PGD_PageDescriptor) sf);
				}
				if (sf instanceof PGP_PagePosition_Format1) {
					handlePGP_PagePosition_Format1((PGP_PagePosition_Format1) sf);
				}
				if (sf instanceof BPT_BeginPresentationTextObject) {
					handleBPT_BeginPresentationTextObject((BPT_BeginPresentationTextObject) sf);
				}
				if (sf instanceof PTX_PresentationTextData) {
					handlePTX_PresentationTextData((PTX_PresentationTextData) sf);
				}
				if (sf instanceof PTD_PresentationTextDataDescriptor_Format2) {
					handlePTD_PresentationTextDataDescriptor_Format2((PTD_PresentationTextDataDescriptor_Format2) sf);
				}
			}
			
		} while (sf != null);
		
		
	}
	
	
	private void dumpFonts() throws IOException {
		
		int fontcounter = 0;
		
		for (FontProperties font : this.fonts) {
		
			String dumpfile = String.format("/tmp/font-%d", fontcounter);

			FileOutputStream os = new FileOutputStream(dumpfile);
			
			os.write(String.format("Nominal size: %d\n", font.getNominalSize()).getBytes());
			os.write(String.format("max box W/H: %dx%d\n", font.getMaxCharacterWidth(), font.getMaxCharacterHeight()).getBytes());
			os.write(String.format("max asc/desc %d/%d\n", font.getMaxAscender(), font.getMaxDescender()).getBytes());
			
			for (String key : font.getGlyphs().keySet()) {
				GlyphProperties p = font.getGlyph(key);
				os.write(String.format("Code point: %d (%c), EDCDIC: %d \n", (int)p.getCodePoint(), (char)p.getCodePoint(), p.getCodePointEBCDIC()).getBytes());
				os.write(String.format("cell dimension: %d(%s)x%d\n", p.getCellWidth(), p.getCharWidth(), p.getCellHeight()).getBytes());
				os.write(String.format(" bytes: %d\n", p.getData().length).getBytes());
				os.write(String.format("character increment %d\n", p.getAdvance()).getBytes());
				os.write(String.format("A-B-C space %d %d %d\n", p.getAspace(), p.getBspace(), p.getCspace()).getBytes());
				os.write(String.format("baseline shift %d\n", p.getBaselineShift()).getBytes());
				os.write(String.format("ascender/descender %d/%d\n", p.getAscender(), p.getDescender()).getBytes());
				
				for (int i=0; i < p.getCellHeight(); i++) {
					for (int j=0; j < p.getCellWidth()/8; j++) {
						
						os.write(bits(p.getData()[ (i*p.getCellWidth()/8) + j]));
					}
					os.write(0x0A);
				}
					
				os.write(0x0A);
				os.write(0x0A);
				
			}
			
			os.close();
			fontcounter += 1;
		}
	}
	
	// return bits as array of characters
	private static byte[] bits(byte b) {
		byte[] bits = new byte[8];
		
		bits[0] = (byte) (((~b & 0x80) == 0) ? '0' : '.'); 
		bits[1] = (byte) (((~b & 0x40) == 0) ? '0' : '.'); 
		bits[2] = (byte) (((~b & 0x20) == 0) ? '0' : '.'); 
		bits[3] = (byte) (((~b & 0x10) == 0) ? '0' : '.'); 
		bits[4] = (byte) (((~b & 0x08) == 0) ? '0' : '.'); 
		bits[5] = (byte) (((~b & 0x04) == 0) ? '0' : '.'); 
		bits[6] = (byte) (((~b & 0x02) == 0) ? '0' : '.'); 
		bits[7] = (byte) (((~b & 0x01) == 0) ? '0' : '.'); 
		
		return bits;
	}
	
	// write bytes to file, flipping every bit
	public static void bytesOut(String file, byte[] bytes) throws IOException {

		FileOutputStream os = new FileOutputStream(file);
		
		int i = 0;
		for (Byte b: bytes) {
			bytes[i++] = (byte) ~b;
		}
		os.write(bytes);

		os.close();
	}
	// return image data, flipping every bit
	public static byte[] flipBits(byte[] data) {

		byte[] bytes =  new byte[data.length];
		int i = 0;
		for (Byte b: data) {
			bytes[i++] = (byte) ~b;
		}
		return bytes;
	}
	
	public int getTextTop() {
		return textTop;
	}
	public void setTextTop(int textTop) {
		this.textTop = textTop;
	}
	public int getTextLeft() {
		return textLeft;
	}
	public void setTextLeft(int textLeft) {
		this.textLeft = textLeft;
	}
	public String getTextData() {
		return textData;
	}
	public void setTextData(String textData) {
		this.textData = textData;
	}
	public int getPagexOffset() {
		return pagexOffset;
	}
	public int getPageyOffset() {
		return pageyOffset;
	}

	public static void main(String[] argv) throws IOException, AFPParserException {
		BaseParser bp = new BaseParser(argv[1]);
		bp.parse();
		bp.close();
	}
}
