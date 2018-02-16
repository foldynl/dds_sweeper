/*
 * FreqRange
 *
 * Author:  Ladislav Foldyna
 *
 * Copyright (C) 2018 Ladislav Foldyna
 *
 * This file is part of AntennaAnalyzer.
 *
 * AntennaAnalyzer is free software: you can redistribute it and/or modify
 * it under the terms of either the Apache Software License, version 2, or
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, version 3 or above.
 *
 * AntennaAnalyzer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of both the GNU Lesser General Public
 * License and the Apache Software License along with jSerialComm. If not,
 * see <http://www.gnu.org/licenses/> and <http://www.apache.org/licenses/>.
 */

public class FreqRange {
	private String band;
	private int freq_low;  // in kHz
	private int freq_high; // in kHz
	
	public FreqRange(String in_band, int in_low, int in_high)
	{
		band = in_band;
		freq_low = in_low;
		freq_high = in_high;
	}
	
	@Override
	public String toString()
	{
		return band;
	}
	
	public int getLowFreq()
	{
		return freq_low;
	}
	
	public int getHighFreq()
	{
		return freq_high;
	}
	

}
