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
 *	represent properties of a medium. 
 *
 */
public class Medium {
	
	String name;
	int xOrigin = 0;
	int yOrigin = 0;

	//inches
	float width = 8.5f;
	float height = 11.0f;
	
	public Medium(String name) {
		this.name = name;
	}


	public int getxOrigin() {
		return xOrigin;
	}
	public void setxOrigin(int xOrigin) {
		this.xOrigin = xOrigin;
	}

	public int getyOrigin() {
		return yOrigin;
	}
	public void setyOrigin(int yOrigin) {
		this.yOrigin = yOrigin;
	}

	public float getWidth() {
		return width;
	}
	public void setWidth(float width) {
		this.width = width;
	}

	public float getHeight() {
		return height;
	}
	public void setHeight(float height) {
		this.height = height;
	}


	public String getName() {
		return name;
	}
}
