package com.cubeStatBuilder.dto

class CubeStats
{
	List<ThemeData> themeData;
	/*
	ok, this one's a duzy because I'm lazy and don't want to find a better way to do this...
					white        blue         black        red          green        multicolored lands        colorless
	planewalkers	[0][0]       [0][1]       [0][2]       [0][3]       [0][4]       [0][5]       [0][6]       [0][7]
	creatures		[1][0]       [1][1]       [1][2]       [1][3]       [1][4]       [1][5]       [1][6]       [1][7]
	instants		[2][0]       [2][1]       [2][2]       [2][3]       [2][4]       [2][5]       [2][6]       [2][7]
	sorceries		[3][0]       [3][1]       [3][2]       [3][3]       [3][4]       [3][5]       [3][6]       [3][7]
	enchantments	[4][0]       [4][1]       [4][2]       [4][3]       [4][4]       [4][5]       [4][6]       [4][7]
	artifacts		[5][0]       [5][1]       [5][2]       [5][3]       [5][4]       [5][5]       [5][6]       [5][7]
	lands			[6][0]       [6][1]       [6][2]       [6][3]       [6][4]       [6][5]       [6][6]       [6][7]
	*/
	int[][] colorTypeAmountMatrix

	CubeStats()
	{
		colorTypeAmountMatrix = [
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0],
			[0, 0, 0, 0, 0, 0, 0, 0]
		]
	}
}