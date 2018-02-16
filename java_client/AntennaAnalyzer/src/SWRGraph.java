/*
 * SWRGraph
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

import java.awt.Color;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.TextAnchor;

public class SWRGraph {
	
	private XYSeries series;
	private XYSeriesCollection dataset;
	private JFreeChart chart;
	private XYPlot plot;
	private ValueAxis freq_range;
	private NumberAxis swr_range;
	
	
	/**
	 * Creates Graph with X-axis range xmin..x_max
	 * and Y-axis range ymin..ymax
	 * 
	 * @param xmin
	 * @param xmax
	 * @param ymin
	 * @param ymax
	 */
	public SWRGraph(int xmin,
					int xmax,
					int ymin,
					int ymax) 
	{
		
		
		series = new XYSeries("VSWR");
		dataset = new XYSeriesCollection(series);

		chart = ChartFactory.createXYLineChart("VSWR", 
				"Frequency [kHz]",
				"SWR", 
				dataset,
				PlotOrientation.VERTICAL, 
				false, 
				false, 
				false);
		
		plot = (XYPlot) chart.getPlot();

		freq_range = plot.getDomainAxis();

		freq_range.setRange(xmin, xmax);

		swr_range = (NumberAxis) plot.getRangeAxis();
		swr_range.setRange(ymin, ymax);
	}
	
	public JFreeChart getChart()
	{
		return chart;
	}

	/**
	 * It changes SWR (Y-axis) range
	 * 
	 * @param swrmin
	 * @param swrmax
	 */
	public void changeSWRRange(int swrmin, int swrmax)
	{
		swr_range.setRange(swrmin, swrmax);
	}
	
	/**
	 * It change Frequency range (X=axis)
	 * 
	 * @param low
	 * @param high
	 */
	public void changeFrequencyRange(int low, int high)
	{
		cleanSWRData();
		freq_range.setRange(low, high);
	}
	
	/**
	 * It adds SWR data to graph
	 * 
	 * @param in_freq
	 * @param in_swr
	 */
	public void addSWRData(float in_freq, float in_swr)
	{
		series.add(in_freq, in_swr);
	}
	
	
	/**
	 * Graph Cleanup 
	 */
	public void cleanSWRData()
	{
		series.clear();
		plot.clearRangeMarkers();
		plot.clearDomainMarkers();
	}
	
	
	/**
	 * Show SWR minimum on graph
	 */
	public void showMinimum()
	{
		Number yminimum = DatasetUtilities.findMinimumRangeValue(dataset);
		ValueMarker ymin = new ValueMarker(yminimum.floatValue());
		ymin.setPaint(Color.orange);
		ymin.setLabel("Min SWR = " + yminimum.floatValue());
		ymin.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
		
		
		Number xval = 0;
		
		for (int seriesIndex = 0 ; seriesIndex < dataset.getSeriesCount() ; seriesIndex++){
	        for (int itemIndex = 0 ; itemIndex < dataset.getItemCount(seriesIndex) ; itemIndex ++){
	            Number yValue = dataset.getY(seriesIndex, itemIndex);
	            if (yValue.equals(yminimum)){
	            	xval = dataset.getX(seriesIndex, itemIndex);
	                ValueMarker xvalue = new ValueMarker(xval.floatValue());
	                xvalue.setPaint(Color.orange);
	                xvalue.setLabel("Freq = " + xval.floatValue());
	                xvalue.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
	                plot.addDomainMarker(xvalue,Layer.BACKGROUND);
	            }
	        }
	    }
		
		plot.addRangeMarker(ymin,Layer.BACKGROUND);
		
	}
	
}
