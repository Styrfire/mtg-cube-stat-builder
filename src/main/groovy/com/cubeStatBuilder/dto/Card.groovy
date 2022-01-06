package com.cubeStatBuilder.dto

import com.cubeStatBuilder.utils.CardType

class Card
{
	String cardName
	int cmc
	CardType cardType

	@Override
	String toString()
	{
		return "cardName = " + cardName +
				"\ncmc = " + cmc +
				"\ncardType = " + cardType
	}
}
