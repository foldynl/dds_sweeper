#!/usr/bin/env python3

# Setup script for calibration Antenna Analyzer project
# the result should has to be inserted to Arduino source code

# Usage
#   ./calibration.py <COM_Port>
#
# Copyright (C) 2018 Ladislav Foldyna
# Author: Ladislav Foldyna

# This file is part of AntennaAnalyzer.
#
# AntennaAnalyzer is free software: you can redistribute it and/or modify
# it under the terms of either the Apache Software License, version 2, or
# the GNU Lesser General Public License as published by the Free Software
# Foundation, version 3 or above.
#
# AntennaAnalyzer is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# You should have received a copy of both the GNU Lesser General Public
# License and the Apache Software License. If not,
# see <http://www.gnu.org/licenses/> and <http://www.apache.org/licenses/>.

import sys
import serial
import numpy as np

def main():
   all_samples = [];

   if ( len(sys.argv) < 2 ):
      sys.stderr.write('Usage: {} COM_PORT\n'.format(sys.argv[0]));
      sys.exit(1);

   com_port = sys.argv[1]

   try:
      ser = serial.Serial(com_port, 57600)
   except serial.SerialException as e:
      sys.stderr.write('Could not open serial port {}: {}\n'.format(com_port, e))
      sys.exit(1);

   # when client establich an connection to arduino, arduino resets itself
   # we will read booting characters but they are not important for calibration

   try:
      while True:
         arduino_answer = str(ser.readline(), 'utf-8').rstrip()
         print(arduino_answer)
         if ( arduino_answer.find('#OK#') >= 0 ):
            break;

      # now calibration loop is starting
      while True:
         command = input("Enter Calibration Load: ")

         if (len(command) == 0 ):
            break;

         command = command + 'f'

         #sending user command
         ser.write(command.encode())

         run_results = []

         #reading SWR calibration samples
         # format is: <expected, frequency, raw_swr>
         while True:
            arduino_answer = str(ser.readline(), 'utf-8').rstrip()
            print(arduino_answer)
            if ( arduino_answer.find('#OK#') >= 0 ):
               break;
            expected_swr, freq, swr = arduino_answer.split(',')
            run_results.append([float(expected_swr), int(freq), float(swr)])

         all_samples.append(run_results)
   except serial.SerialException as e:
      sys.stderr.write('Communication error {}\n'.format(e))
      sys.exit(1)

   #computing the coefficients
   for i in range(0,30):
      x = [b[2] for b in [item[i] for item in all_samples]]
      y = [b[0] for b in [item[i] for item in all_samples]]
      z = np.polyfit(x,y,3)
      print("{ .do_correction = 1, .coef3 = %f, .coef2 = %f, .coef1 = %f, .con = %f}," % (z[0], z[1], z[2], z[3]) )

if __name__ == '__main__':
   main()
