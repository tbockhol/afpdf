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

/**
 * properties of a single glyph
 */
public class GlyphProperties {
	
	String gcgid;
	char codePoint;
	int codePointEBCDIC;
	int cellWidth;// cell width is padded to 8
	int cellHeight;
	int charWidth; // width of character only
	int ascender;
	int descender;
	short aspace;
	short bspace;
	short cspace;
	short baselineShift;
	short advance;
	byte[] data;
	
	public GlyphProperties(String gcgid, byte[] data, int cellWidth, int cellHeight) {
		this.data = data;
		this.cellWidth = cellWidth;
		this.cellHeight = cellHeight;
	}

	public char getCodePoint() {
		return codePoint;
	}
	public void setCodePoint(char codePoint) {
		this.codePoint = codePoint;
	}

	public short getAspace() {
		return aspace;
	}
	public void setAspace(short aspace) {
		this.aspace = aspace;
	}

	public short getBspace() {
		return bspace;
	}
	public void setBspace(short bspace) {
		this.bspace = bspace;
	}

	public short getCspace() {
		return cspace;
	}
	public void setCspace(short cspace) {
		this.cspace = cspace;
	}

	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	
	/*
	 * just the data that makes up the character, without extra padding on right
	 */
	public byte[] getCharacterData() {
		return null;
	}
	
	public int getCharacterHeight() {
		return getAscender() + getDescender();
	}

	public short getBaselineShift() {
		return baselineShift;
	}
	public void setBaselineShift(short baselineShift) {
		this.baselineShift = baselineShift;
	}

	public short getAdvance() {
		return advance;
	}
	public void setAdvance(short advance) {
		this.advance = advance;
	}

	public int getCellHeight() {
		return cellHeight;
	}
	public void setCellHeight(int cellHeight) {
		this.cellHeight = cellHeight;
	}

	public int getCellWidth() {
		return cellWidth;
	}
	public void setCellWidth(int cellWidth) {
		this.cellWidth = cellWidth;
	}

	public int getCharWidth() {
		return charWidth;
	}
	public void setCharWidth(int charWidth) {
		this.charWidth = charWidth;
	}

	public int getCodePointEBCDIC() {
		return codePointEBCDIC;
	}
	public void setCodePointEBCDIC(int codePointEBCDIC) {
		this.codePointEBCDIC = codePointEBCDIC;
	}

	public int getAscender() {
		return ascender;
	}
	public void setAscender(int ascender) {
		this.ascender = ascender;
	}

	public int getDescender() {
		return descender;
	}
	public void setDescender(int descender) {
		this.descender = descender;
	}
}
