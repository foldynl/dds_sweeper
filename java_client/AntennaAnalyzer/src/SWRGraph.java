
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

public class SWRGraph implements ChartMouseListener {

	public enum SERIE_TYPE {
		PRIMARY, SECONDARY
	};

	private XYSeries series;
	private XYSeries series2;
	private XYSeriesCollection dataset;
	private JFreeChart chart;
	private XYPlot plot;
	private ValueAxis freq_range;
	private NumberAxis swr_range;
	private ChartPanel chartPanel;
	private Crosshair xCrosshair;
	private Crosshair yCrosshair;

	/**
	 * Creates Graph with X-axis range xmin..x_max and Y-axis range ymin..ymax
	 * 
	 * @param xmin
	 * @param xmax
	 * @param ymin
	 * @param ymax
	 */
	public SWRGraph(int xmin, int xmax, int ymin, int ymax) {

		series = new XYSeries("VSWR");
		series2 = new XYSeries("VSWR 2nd");

		dataset = new XYSeriesCollection(series);
		dataset.addSeries(series2);

		chart = ChartFactory.createXYLineChart("VSWR", "Frequency [kHz]", "SWR", dataset, PlotOrientation.VERTICAL,
				true, true, true);

		plot = (XYPlot) chart.getPlot();

		freq_range = plot.getDomainAxis();

		freq_range.setRange(xmin, xmax);

		swr_range = (NumberAxis) plot.getRangeAxis();
		swr_range.setRange(ymin, ymax);

		/* Creates Overlay */
		chartPanel = new ChartPanel(getChart(), true, true, true, true, true);
		CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
		xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
		xCrosshair.setLabelVisible(true);
		yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
		yCrosshair.setLabelVisible(true);
		crosshairOverlay.addDomainCrosshair(xCrosshair);
		crosshairOverlay.addRangeCrosshair(yCrosshair);

		chartPanel.addOverlay(crosshairOverlay);

		chartPanel.addChartMouseListener(this);
	}

	private int SERIETYPE2Index(SERIE_TYPE type) {
		if (type == SERIE_TYPE.PRIMARY) {
			return 0;
		}
		return 1;
	}

	public ChartPanel getChartPanel() {
		return chartPanel;
	}

	public JFreeChart getChart() {
		return chart;
	}

	/**
	 * It changes SWR (Y-axis) range
	 * 
	 * @param swrmin
	 * @param swrmax
	 */
	public void changeSWRRange(int swrmin, int swrmax) {
		swr_range.setRange(swrmin, swrmax);
	}

	/**
	 * It change Frequency range (X=axis)
	 * 
	 * @param low
	 * @param high
	 */
	public void changeFrequencyRange(int low, int high) {
		cleanSWRData(SERIE_TYPE.PRIMARY);
		cleanSWRData(SERIE_TYPE.SECONDARY);
		freq_range.setRange(low, high);
	}

	/**
	 * It adds SWR data to graph
	 * 
	 * @param in_freq
	 * @param in_swr
	 */
	public void addSWRData(SERIE_TYPE in_serie, float in_freq, float in_swr) {
		dataset.getSeries(SERIETYPE2Index(in_serie)).add(in_freq, in_swr);
	}

	/**
	 * Graph Cleanup
	 */
	public void cleanSWRData(SERIE_TYPE in_serie) {

		/* we show markers only for primary serie */
		if (in_serie == SERIE_TYPE.PRIMARY) {
			series.clear();
			plot.clearRangeMarkers();
			plot.clearDomainMarkers();
		} else {
			series2.clear();
		}
	}

	/**
	 * Show SWR minimum on graph
	 */
	public void showMinimum(SERIE_TYPE in_serie) {
		int seriesIndex = SERIETYPE2Index(in_serie);
		XYSeriesCollection local_serie = new XYSeriesCollection(dataset.getSeries(seriesIndex));

		Number yminimum = DatasetUtilities.findMinimumRangeValue(local_serie);
		ValueMarker ymin = new ValueMarker(yminimum.floatValue());
		ymin.setPaint(Color.orange);
		ymin.setLabel("Min SWR = " + yminimum.floatValue());
		ymin.setLabelTextAnchor(TextAnchor.CENTER_LEFT);

		Number xval = 0;

		for (int itemIndex = 0; itemIndex < local_serie.getItemCount(0); itemIndex++) {
			Number yValue = local_serie.getY(seriesIndex, itemIndex);
			if (yValue.equals(yminimum)) {
				xval = local_serie.getX(seriesIndex, itemIndex);
				ValueMarker xvalue = new ValueMarker(xval.floatValue());
				xvalue.setPaint(Color.orange);
				xvalue.setLabel("Freq = " + xval.floatValue());
				xvalue.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
				plot.addDomainMarker(xvalue, Layer.BACKGROUND);
			}
		}

		plot.addRangeMarker(ymin, Layer.BACKGROUND);

	}

	/**
	 * It saves the Graph to JPG file
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void saveAsCSV(String filename, int in_serie) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		DecimalFormat f = new DecimalFormat("#.##");
		out.write("Frequence_HZ, SWR");
		out.newLine();
		for (int itemIndex = 0; itemIndex < dataset.getItemCount(in_serie); itemIndex++) {
			Number freq = dataset.getX(in_serie, itemIndex);
			Number swr = dataset.getY(in_serie, itemIndex);

			out.write((int) (freq.floatValue() * 1000) + "," + f.format(swr));
			out.newLine();
		}
		out.close();

	}

	@Override
	public void chartMouseClicked(ChartMouseEvent arg0) {
		// ignore it

	}

	@Override
	public void chartMouseMoved(ChartMouseEvent event) {
		Rectangle2D dataArea = chartPanel.getScreenDataArea();
		JFreeChart chart = event.getChart();
		XYPlot plot = (XYPlot) chart.getPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
		double y = DatasetUtilities.findYValue(plot.getDataset(), 0, x);
		xCrosshair.setValue(x);
		yCrosshair.setValue(y);

	}

}
