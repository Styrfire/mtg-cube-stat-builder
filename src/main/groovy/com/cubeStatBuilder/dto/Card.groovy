package com.cubeStatBuilder.dto

import com.cubeStatBuilder.utils.CardType
import com.cubeStatBuilder.utils.Color

class Card
{
	String cardName
	int cmc
	List<Color> colors
	CardType cardType

	@Override
	String toString()
	{
//		String colorsStr = ""
//		for (Color color : colors)
//		{
//			if (colorsStr == "")
//				colors += color
//			else
//				colors += " " + color
//		}

		return "cardName = " + cardName +
				"\ncolors = " + colors.toString() +
				"\ncmc = " + cmc +
				"\ncardType = " + cardType
	}
}
