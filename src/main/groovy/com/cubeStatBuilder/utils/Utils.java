package com.cubeStatBuilder.utils;

import com.cubeStatBuilder.dto.Card;
import com.cubeStatBuilder.dto.CubeStats;
import com.cubeStatBuilder.dto.ThemeData;

import java.util.List;

public class Utils
{
	public static int[][] getColorTypeAmountMatrixFromCardList(List<Card> cards)
	{
		// initialize colorTypeAmountMatrix
		int[][] colorTypeAmountMatrix = {
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0},
			{0, 0, 0, 0, 0, 0, 0, 0}
		};

		for (Card card : cards)
		{
			int row;
			int column;

			switch (card.getCardType())
			{
				case Planeswalker:
					row = 0;
					break;
				case Creature:
					row = 1;
					break;
				case Instant:
					row = 2;
					break;
				case Sorcery:
					row = 3;
					break;
				case Enchantment:
					row = 4;
					break;
				case Artifact:
					row = 5;
					break;
				case Land:
					row = 6;
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + card.getCardType());
			}

			if (card.getColors().size() != 1)
				column = 5;
			else
			{
				switch (card.getColors().get(0))
				{
					case White:
						column = 0;
						break;
					case Blue:
						column = 1;
						break;
					case Black:
						column = 2;
						break;
					case Red:
						column = 3;
						break;
					case Green:
						column = 4;
						break;
					case Land:
						column = 6;
						break;
					case Colorless:
						column = 7;
						break;
					default:
						throw new IllegalStateException("Unexpected value: " + card.getColors().toString());
				}
			}

			colorTypeAmountMatrix[row][column] += 1;
		}

		return colorTypeAmountMatrix;
	}

	public static CubeStats getCubeStatsFromThemeData(List<ThemeData> themeDataList)
	{
		CubeStats cubeStats = new CubeStats();
		// initialize colorTypeAmountMatrix
		int[][] colorTypeAmountMatrix = {
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0}
		};

		// for each theme, add the colorTypeMatrixData together.
		for (ThemeData themeData : themeDataList)
			for (int i = 0; i < 7; i++)
				for (int j = 0; j < 8; j++)
					colorTypeAmountMatrix[i][j] += themeData.getColorTypeAmountMatrix()[i][j];

		cubeStats.setThemeData(themeDataList);
		cubeStats.setColorTypeAmountMatrix(colorTypeAmountMatrix);

		return cubeStats;
	}
}
