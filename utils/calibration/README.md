Based on the measurement, Analyzer did not provide accurate SWR results. The raw SWR
was very dependent on frequency and did not match an expected SWR.

The raw SWR samples are shown below:

Freq[Hz]/Expected SWR		1	1,5	2	3	4	7,8	20
3000000				1,00517	1,14305	1,55263	2,43736	3,28571	6,40952	16,12222
7000000				1,00504	1,14516	1,53968	2,38526	3,15544	5,93103	13,83333
9000000				1,00482	1,15789	1,53251	2,32927	3,07463	5,6129	11,98413
10500000			1,00442	1,14634	1,49639	2,24953	2,92593	5,20588	10,2755
11500000			1,0042	1,13889	1,46739	2,14992	2,76547	4,77483	8,95402
14000000			1,00386	1,1346	1,41396	1,97744	2,47143	3,88776	6,27692
21000000			1,00377	1,1477	1,40177	1,90788	2,3211	3,41633	5,03364
28000000			5,8951	1,1529	1,43052	2,02791	2,48699	3,83	5,8951

Therefore a calibration is highly recommended. It was tested many interpolation techniques with
various results. Very negative think was a strong frequency dependence. Therefore the
Antenna analyzer bandwidth (1-30MHz) was divided into the chunks - 30 chunks; 1per 1MHz

After bandwidth splitting, it was possible to analyze the SWR curve and find a technique
for computing real (expected) SWR from raw SWR. The best interpolation result returned a polynominal
function order 3

Folowing formula shows how the rela SWR is computed by Antenna Analyzer.

real SWR = coef3 * RAW_SWR^3 + coef2 * RAW_SWR^2 + coef1 * RAW_SWR + con

where coef3..1 and con are pre-computed values for each chunk. The coefficients are computed by calibration.py
and have to be copy&pasted to arduino source code.

What does the utility calibration.py do:

1) calibration.py connects the Antenna Analyzer
2) User is asked to enter a calibration Load
   example "500" means: user has connected 500 Ohm dummy load and calibration can start
3) calibration.py receives SWR values from Antenna Analyzer for every bandwidth chunk
4) User should repeat steps 2 and 3 with various dummy loads - recommended dummy load range is 50 - 1000 Ohm
5) If user enters an empty string then the utility computes the coefficients.
6) The coefficients are printed as C-array initialization structure.
7) The C-array initialization structure has to be copy&pasted to arduino source code - initialization part
8) The arduino source code has to be compiled and uploaded to Arduino.

