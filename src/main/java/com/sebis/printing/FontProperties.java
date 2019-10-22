package com.sebis.printing;
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
import java.util.HashMap;
import java.util.Map;

/**
 * hold font properties and glyph data
 */
public class FontProperties {
	
	
	int nominalSize; // in points
	int maxCharacterHeight;
	int maxCharacterWidth;
	int maxAscender;
	int maxDescender;
	String codePageName;
	// gcgid key
	Map<String, GlyphProperties> glyphs;
	
	public FontProperties() {
		glyphs = new HashMap<String, GlyphProperties>();
	}

	public void addGlyph(String gcgid, GlyphProperties glyph) {
		this.glyphs.put(gcgid, glyph);
	}
	public Map<String, GlyphProperties> getGlyphs() {
		return this.glyphs;
	}
	public GlyphProperties getGlyph(String gcgid) {
		return this.glyphs.get(gcgid);
	}
	
	public int getNominalSize() {
		return nominalSize;
	}
	public void setNominalSize(int nominalSize) {
		this.nominalSize = nominalSize;
	}

	public int getMaxCharacterHeight() {
		return maxCharacterHeight;
	}
	public void setMaxCharacterHeight(int maxCharacterHeight) {
		this.maxCharacterHeight = maxCharacterHeight;
	}

	public int getMaxCharacterWidth() {
		return maxCharacterWidth;
	}
	public void setMaxCharacterWidth(int maxCharacterWidth) {
		this.maxCharacterWidth = maxCharacterWidth;
	}

	public int getMaxAscender() {
		return maxAscender;
	}
	public void setMaxAscender(int maxAscender) {
		this.maxAscender = maxAscender;
	}

	public int getMaxDescender() {
		return maxDescender;
	}
	public void setMaxDescender(int maxDescender) {
		this.maxDescender = maxDescender;
	}

	public String getCodePageName() {
		return codePageName;
	}

	public void setCodePageName(String codePageName) {
		this.codePageName = codePageName;
	}
}
