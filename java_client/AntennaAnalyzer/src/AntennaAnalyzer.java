/*
 * AntennaAnalyzer
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

import java.awt.event.ActionEvent;
import javax.swing.event.*;

import java.awt.event.ActionListener;	
import javax.swing.*;
import java.awt.*;
import org.jfree.chart.ChartPanel;

import com.fazecast.jSerialComm.SerialPort;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class AntennaAnalyzer extends JFrame  {

	private static final long serialVersionUID = 1L;
	
	private JComboBox<String> portList; 
	private JComboBox<String> bandList;
	
	private JButton connectButton;
	private JButton sweepButton;
	
	private JTextField start_freq; 
	private JTextField stop_freq;
	
	private JSlider sliderSWR;
	
	private SWRGraph swrGraph;
	
	private JTextArea arduinoMonitor;
	
	private InputFreqVerifier freqVerifier;
	
	private ArduinoAnalyzerCom arduino;
	
	Map<String, FreqRange> bandPlan = new TreeMap<String, FreqRange>();
	
    /**
     * Returns the lowest frequency for selected band
     * 
     * @return The lowest frequency 
     */
    private int getSelectLowFreq()
	{
	   return bandPlan.get(bandList.getSelectedItem()).getLowFreq();
    }
    
    /**
     * Returns the highest frequency for selected band
     * 
     * @return The highest frequency 
     */    
	private int getSelectHighFreq()
	{
	   return bandPlan.get(bandList.getSelectedItem()).getHighFreq();
	}

	/**
	 * Handles SWR Zoom slider change Event. Modifies the SWR range in Graph
	 * 
	 * @param e Change Event from Slider
	 */
	private void sliderSWR_change(ChangeEvent e)
	{
	   JSlider source = (JSlider)e.getSource();
	   if (!source.getValueIsAdjusting())
	   {
		   swrGraph.changeSWRRange(1, source.getValue());
	   }
	}

    /**
     * Handles Band list Event.
     * 
     * @param event
     */
    private void bandListAction(ActionEvent event)
    {
		String selectedband = (String) bandList.getSelectedItem();

		if (selectedband.equals("custom"))
		{
			start_freq.setEditable(true);
			stop_freq.setEditable(true);
		}
		else
		{
			start_freq.setEditable(false);
			stop_freq.setEditable(false);
		}
		
		swrGraph.changeFrequencyRange(getSelectLowFreq(), getSelectHighFreq());
		start_freq.setText(String.valueOf(getSelectLowFreq()));
		stop_freq.setText(String.valueOf(getSelectHighFreq()));
    }

    /**
     * Handles Sweep Button Action
     * 
     * @param event
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    private synchronized void sweepButtonAction(ActionEvent event) throws InterruptedException, ExecutionException, TimeoutException
	{
		String cmd;
		Float in_freq;
		Float in_swr;
		String tokens[];
		String str;
		Float tmp = (float) 0.3;

        swrGraph.cleanSWRData();

		try {
			cmd = String.format("%da", (Integer.parseInt(start_freq.getText())) * 1000);

			arduino.sendOneShotCommand(cmd, 1000);

			cmd = String.format("%db", (Integer.parseInt(stop_freq.getText())) * 1000);

			arduino.sendOneShotCommand(cmd, 1000);

			arduino.sendCommand("s");
            repaint();
		} catch (Exception e) {
			System.out.println(e);
		};

		
		while (!(str = arduino.nextLineTimeout(5000)).equals("#OK#")) 
		{
			try {
				tokens = str.split(",");
				in_freq = Float.parseFloat(tokens[0]);
				in_freq = in_freq / 1000;
				in_swr = Float.parseFloat(tokens[1]);
				tmp = tmp + (float) 0.3;
				swrGraph.addSWRData(in_freq, in_swr);
				repaint();

			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		swrGraph.showMinimum();
	}
   
    /**
     * Handles Connect Button Action
     * 
     * @param e
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    private synchronized void connectButtonAction(ActionEvent e) throws InterruptedException, ExecutionException, TimeoutException
    {
    	String str;
    	
    	if (connectButton.getText().equals("Connect")) {

			if ( portList.getSelectedItem() == null)
			{
				System.out.println("No port selected");
				return;
			}
			// attempt to connect to the serial port
			
			
			if ( arduino.openPort(portList.getSelectedItem().toString()) )
			{
			    portList.setEnabled(false);
			

				while (!(str = arduino.nextLineTimeout(5000)).equals("#OK#")) 
				{
					try 
					{
						//System.out.println(str);
					} catch (Exception et) 
					{
						portList.setEnabled(true);
						arduino.closePort();
						System.out.println(et.getMessage());
						throw et;
					}
					repaint();
				}

				connectButton.setBackground(Color.GREEN);
				connectButton.setText("Disconnect");
				sweepButton.setEnabled(true);
			}
			else
			{
			   System.out.println("Cannot Open Port");
			}
		}
		else
		{
			//disconnect
			arduino.closePort();
			portList.setEnabled(true);
			connectButton.setText("Connect");
			connectButton.setBackground(Color.RED);
			sweepButton.setEnabled(false);
		}
    }

    /**
     * Handles frequency input Text Input
     * @param e
     */
    private void freqInputAction(ActionEvent e)
    {
    	JTextField curr_field = (JTextField) e.getSource();
    	    	
    	if ( freqVerifier.verify(curr_field))
    	{

    	   int low = Integer.parseInt(start_freq.getText());
    	   int high = Integer.parseInt(stop_freq.getText());

    	   if ( low < high )
    	   {
    	      swrGraph.changeFrequencyRange(low, high);
    	      
    	   }
    	   else
    	   {
    		   JOptionPane.showMessageDialog( 
     	              null, "Invalid Frequency Range", "Invalid input", JOptionPane.ERROR_MESSAGE); 
    		   curr_field.selectAll();
    	   }
    	}
    }
    
    /**
     * Creates TOP Panel
     * 
     * @return created panel
     */
    private JPanel createNorthPanel()
    {
    	JPanel localPanel =  new JPanel(new FlowLayout(FlowLayout.LEFT));
    	
		/*
		 * COM port Combo Box
		 */
		portList = new JComboBox<String>();
		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for (int i = 0; i < portNames.length; i++)
		{
		   portList.addItem(portNames[i].getSystemPortName());
		}
		
		/*
		 * COM Port Connect BUTTON
		 */
		connectButton= new JButton("Connect");
		connectButton.setBackground(Color.RED);
		// configure the connect button and use another thread to listen for data
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg) {
				Thread thread = new Thread(){
					@Override 
					public void run() {
						try {
							connectButtonAction(arg);
						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
			   thread.start();
			}
		});
		/*
		 * Band List Combo
		 */
		bandList = new JComboBox<String>();
		
		for (Map.Entry<String, FreqRange> entry : bandPlan.entrySet())
		{
			bandList.addItem(entry.getKey());
		}

		bandList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				bandListAction(event);
			}
		});
		/* 
		 * Frequency range
		 */
		start_freq = new JTextField(5);
		start_freq.setText(String.valueOf(getSelectLowFreq()));
		start_freq.setEditable(false);	
		start_freq.setInputVerifier(freqVerifier);
		start_freq.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				freqInputAction(event);
		    }
		});
		

		stop_freq = new JTextField(5);
		stop_freq.setText(String.valueOf(getSelectHighFreq()));
		stop_freq.setEditable(false);
		stop_freq.setInputVerifier(freqVerifier);
		stop_freq.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    freqInputAction(event);
		    }
		});
		
		/*
		 * Sweep Button
		 */
		
		sweepButton = new JButton("Sweep");
		sweepButton.setEnabled(false);
		sweepButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg) {
					//sweepButtonAction(arg);
				Thread thread = new Thread(){
					@Override 
					public void run() {
						try {
							sweepButtonAction(arg);
						} catch (InterruptedException | ExecutionException | TimeoutException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
			   thread.start();
			}
		});
		
		/*
		 * Prepare Panel
		 */
		localPanel.add(new JLabel("COM Port:"));
		localPanel.add(portList);
		localPanel.add(connectButton);
		localPanel.add(Box.createRigidArea(new Dimension(30, 0)));
		localPanel.add(new JLabel("Band: "));
		localPanel.add(bandList);
		localPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		localPanel.add(new JLabel("Start: "));
		localPanel.add(start_freq);
		localPanel.add(new JLabel("  End: "));
		localPanel.add(stop_freq);
		localPanel.add(new JLabel("kHz"));
		localPanel.add(Box.createRigidArea(new Dimension(2, 0)));
		localPanel.add(sweepButton);
		
		return localPanel;
    }
    
    /**
     * Creates Central Panel layout
     * 
     * @return created panel
     */
    private JPanel createCenterPanel()
    {
    	JPanel localPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    	ChartPanel chartPanel;
    	
    	/*
    	 * SWR Zoom Slider
    	 */
    	sliderSWR = new JSlider(JSlider.VERTICAL,2, 10, 2);
		sliderSWR.setMajorTickSpacing(8);
		sliderSWR.setMinorTickSpacing(1);
		sliderSWR.setPaintLabels(true);
		sliderSWR.setPaintTicks(true);
		sliderSWR.setPreferredSize(new Dimension(50, 700));
		sliderSWR.setLabelTable(sliderSWR.createStandardLabels(1));
		
	    sliderSWR.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e)
			{
				sliderSWR_change(e);
			}
	    });
		/*
		 * SWR Graph
		 */
    	swrGraph = new SWRGraph(getSelectLowFreq(), getSelectHighFreq(), 1, 2);
    	chartPanel = new ChartPanel(swrGraph.getChart(), true,true,true,true, true);
    	chartPanel.setPreferredSize(new Dimension(1200,800));
    	
		/*
		 * Prepare Panel
		 */
    	localPanel.add(sliderSWR);
    	localPanel.add(chartPanel);
    	
    	return localPanel;
    }
	
    
    /**
     * It create South Panel (Arduino Monitor)
     * 
     * @return created Panel
     */
    private JPanel createSouthPanel()
    {
    	JPanel localPanel =  new JPanel(new FlowLayout(FlowLayout.LEFT));
    	
    	arduinoMonitor = new JTextArea();
    	arduinoMonitor.setEditable(false);

    	JScrollPane scrollingArea = new JScrollPane(arduinoMonitor);
    	scrollingArea.setPreferredSize(new Dimension(1255,100));
    	scrollingArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    	
    	localPanel.add(scrollingArea);
    	return localPanel;
    	
    }
    
	/**
	 * Constructor for Antenna Analyzer
	 * 
	 * @param title  Window Title
	 */
	public AntennaAnalyzer(String title)
	{
		super(title);
		setSize(1280, 1024);
		setLayout(new BorderLayout());
		setVisible(true);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		bandPlan.put("160m",   new FreqRange("160m",   1800,2000));
		bandPlan.put("80m",    new FreqRange("80m",    3500,3800));
		bandPlan.put("40m",    new FreqRange("40m",    7000,7200));
		bandPlan.put("20m",    new FreqRange("20m",    14000,14350));
		bandPlan.put("10m",    new FreqRange("10m",    28000,29700));
		bandPlan.put("18m",    new FreqRange("18m",    18068, 18168));
		bandPlan.put("custom", new FreqRange("custom", 1000,30000));
		
		freqVerifier = new InputFreqVerifier();
		
		add(createNorthPanel(), BorderLayout.NORTH);
		add(createCenterPanel(), BorderLayout.CENTER);
		add(createSouthPanel(), BorderLayout.SOUTH);
		
		arduino = new ArduinoAnalyzerCom(57600, arduinoMonitor);
		
		

	}
	
    /**
     * Main Loop
     * 
     * @param args
     */
    public static void main(String[] args) {
		  SwingUtilities.invokeLater(new Runnable() {
		   
		   @Override
		   public void run() {
		    new AntennaAnalyzer("Antenna Analyzer");
		   }
		  });
		
	}
}