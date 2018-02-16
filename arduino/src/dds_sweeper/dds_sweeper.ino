/***************************************************************************\
    Name    : DDS_Sweeper
    Author  : Beric Dunn (K6BEZ)
    Notice  : Copyright (c) 2013  CC-BY-SA
            : Creative Commons Attribution-ShareAlike 3.0 Unported License

    Notes   : Antenna analyzer - DDS and Arduino
            : Arduino Pins mapping
            :    A0 - Reverse Detector Analog in
            :    A1 - Forward Detector Analog in


            : Modified by Norbert Redeker (DG7EAO) 07/2014
            : Modified by Heribert Schulte dk2jk 09/2014 //(hs)
            : Modified by Ladislav Foldyna OK1MLG 2018

            : OK1MLG Versions Changelog:
               3.1lf - added calibration support,
                       variable type optimalizations
                       't' option was removed
                       'c' option was changed - new meaning
                       'f' new option - for SWR calibration
               3.0lf - removed morse module,
                       added median-style calibration of Analog input,
                       code style modifications,
                       changed Sweep output format,
                       added a command confirmation string "#OK#"
  \***************************************************************************/

const char  SoftwareVersion[] = "Version: 3.1lf";

#define pulseHigh(pin) {digitalWrite(pin, HIGH); digitalWrite(pin, LOW); }

// Struct stores SWR calibration coefficients
typedef struct
{
  byte do_correction;   // Should algorithm fix SWR?
                        // 1 = Yes; otherwise = No

  double coef3;          // coef number 3
  double coef2;          // coef number 2
  double coef1;          // coef number 1
  double con;            // coef number 0
} PolynomCoefs;

 /*
  * The array below defines a correction coefficient matrix. Each row means
  * 1MHz interval like this
  *    - index 0 means 0 - 1MHz
  *    - index 1 means 1 - 2MHz ...
  *
  * Each row containts 4 correction coefficients where final SWR is computed as a result
  * of following formula:
  *
  *  SWR = coef3 * RAW_SWR^3 + coef2 * RAW_SWR^2 + coef1 * RAW_SWR + con
  *
  *  The coefficients below were computed by external utility based on the result of 'f' command.
  *  For more information about the calibration, refer documentation for calibration utility
  */
#define NUM_CALIBRATION_INTERVALS 31
const PolynomCoefs calib_coefs[NUM_CALIBRATION_INTERVALS] =
{
  { .do_correction = 1, .coef3 = -0.000964, .coef2 = 0.015756, .coef1 = 1.194757, .con = -0.005876},
  { .do_correction = 1, .coef3 = 0.001645, .coef2 = -0.036698, .coef1 = 1.394098, .con = -0.180184},
  { .do_correction = 1, .coef3 = 0.000899, .coef2 = -0.019816, .coef1 = 1.330523, .con = -0.129311},
  { .do_correction = 1, .coef3 = 0.001750, .coef2 = -0.031022, .coef1 = 1.372468, .con = -0.164633},
  { .do_correction = 1, .coef3 = 0.001042, .coef2 = -0.023993, .coef1 = 1.376893, .con = -0.184829},
  { .do_correction = 1, .coef3 = 0.000177, .coef2 = -0.003708, .coef1 = 1.310016, .con = -0.128301},
  { .do_correction = 1, .coef3 = 0.001649, .coef2 = -0.023215, .coef1 = 1.400612, .con = -0.218040},
  { .do_correction = 1, .coef3 = 0.002490, .coef2 = -0.026770, .coef1 = 1.429066, .con = -0.246352},
  { .do_correction = 1, .coef3 = 0.002922, .coef2 = -0.022296, .coef1 = 1.442948, .con = -0.270416},
  { .do_correction = 1, .coef3 = 0.003529, .coef2 = -0.017217, .coef1 = 1.453895, .con = -0.286308},
  { .do_correction = 1, .coef3 = 0.007028, .coef2 = -0.024068, .coef1 = 1.519851, .con = -0.345758},
  { .do_correction = 1, .coef3 = 0.018508, .coef2 = -0.107140, .coef1 = 1.836953, .con = -0.610146},
  { .do_correction = 1, .coef3 = 0.029591, .coef2 = -0.131843, .coef1 = 1.944062, .con = -0.705295},
  { .do_correction = 1, .coef3 = 0.069384, .coef2 = -0.363137, .coef1 = 2.505017, .con = -1.098422},
  { .do_correction = 1, .coef3 = 0.083189, .coef2 = -0.338165, .coef1 = 2.428765, .con = -1.054471},
  { .do_correction = 1, .coef3 = 0.136076, .coef2 = -0.604922, .coef1 = 2.953907, .con = -1.389681},
  { .do_correction = 1, .coef3 = 0.165383, .coef2 = -0.709227, .coef1 = 3.107269, .con = -1.465635},
  { .do_correction = 1, .coef3 = 0.195781, .coef2 = -0.873948, .coef1 = 3.437081, .con = -1.682495},
  { .do_correction = 1, .coef3 = 0.232297, .coef2 = -1.086681, .coef1 = 3.820576, .con = -1.895017},
  { .do_correction = 1, .coef3 = 0.256462, .coef2 = -1.241932, .coef1 = 4.126300, .con = -2.079333},
  { .do_correction = 1, .coef3 = 0.220646, .coef2 = -0.992293, .coef1 = 3.609014, .con = -1.766855},
  { .do_correction = 1, .coef3 = 0.253791, .coef2 = -1.217828, .coef1 = 4.051844, .con = -2.029558},
  { .do_correction = 1, .coef3 = 0.224653, .coef2 = -1.048951, .coef1 = 3.726836, .con = -1.841419},
  { .do_correction = 1, .coef3 = 0.221744, .coef2 = -1.055240, .coef1 = 3.736445, .con = -1.840192},
  { .do_correction = 1, .coef3 = 0.186079, .coef2 = -0.823459, .coef1 = 3.232868, .con = -1.516395},
  { .do_correction = 1, .coef3 = 0.208819, .coef2 = -1.068261, .coef1 = 3.786621, .con = -1.861766},
  { .do_correction = 1, .coef3 = 0.165686, .coef2 = -0.825676, .coef1 = 3.326519, .con = -1.592003},
  { .do_correction = 1, .coef3 = 0.122054, .coef2 = -0.536251, .coef1 = 2.690653, .con = -1.177502},
  { .do_correction = 1, .coef3 = 0.098520, .coef2 = -0.424684, .coef1 = 2.468258, .con = -1.032900},
  { .do_correction = 1, .coef3 = 0.099823, .coef2 = -0.503191, .coef1 = 2.676104, .con = -1.160921},
  { .do_correction = 1, .coef3 = 0.099823, .coef2 = -0.503191, .coef1 = 2.676104, .con = -1.160921}
};

// Number of samples used by an analog input calibration procedure
#define CALIBRATION_SAMPLE_SZ 16

/* ================ DO NOT CHANGE THESE CONSTANTS ================*/
// board mapping
const uint8_t SCLK  = 12;
const uint8_t FQ_UD = 11;
const uint8_t SDAT  = 10;
const uint8_t RESET = 9;

unsigned long freq_start = 1.0E6;        // Start Frequency for sweep
unsigned long freq_stop = 30.0E6;        // Stop Frequency for sweep
unsigned long serial_input_number = 0L;  // Used to build number from serial stream
unsigned int num_steps = 100;            // Number of steps to use in the sweep

int offset_forward = 0;      // analog input voltage offset - used for voltage correction
int offset_reverse = 0;      // analog input voltage offset - used for voltage correction

/* ================  END OF "DO NOT CHANGE THESE CONSTANTS ================*/

/***************
 * Initial Setup
 ***************/
void setup()
{
  // Configure DDS control pins for digital output
  pinMode(FQ_UD, OUTPUT);
  pinMode(SCLK, OUTPUT);
  pinMode(SDAT, OUTPUT);
  pinMode(RESET, OUTPUT);

  // Configure LED pin for digital output
  pinMode(13, OUTPUT);

  // Set up analog inputs on A0 and A1, internal reference voltage
  pinMode(A0, INPUT);
  pinMode(A1, INPUT);
  analogReference(INTERNAL); // ref voltage = 1,1 volt

  // initialize serial communication at 57600 baud
  Serial.begin(57600);

  // Reset the DDS
  pulseHigh(RESET);

  //Initialise the incoming serial number to zero
  serial_input_number = 0;

  //Calibrate analog input
  delay(1000); // it seems that arduino needes to relax before an analog input calibration;
  calc_analog_input_offsets();
  endOfCommand();

  Serial.flush();
}

/******************
 * Main Event Loop
 ******************/
void loop()
{
  //Check for character
  if ( Serial.available() > 0 )
  {
    interpreter(Serial.read());
    Serial.flush();
  }
}

/***********************************
 * Easy Command Line Interpreter
 *
 * Input: rxd - character from input
 **********************************/
void interpreter(char rxd)
{
  switch (rxd)
  {
    /*************************
     * Frequency Digits Input
     */
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
      serial_input_number = serial_input_number * 10 + (rxd - '0');
      break;

    /*********************
     * Set Start Frequency
     *
     * Example: 7000000A
     */
    case 'A':
    case 'a':
      freq_start = validate_input_freq(serial_input_number);
      serial_input_number = 0L;
      endOfCommand();
      break;

    /*********************
     * Set Stop Frequency
     *
     * Example: 70000000B
     */
    case 'B':
    case 'b':
      freq_stop = validate_input_freq(serial_input_number);
      serial_input_number = 0L;
      endOfCommand();
      break;

    /********************************
     * Turn frequency into freq_start
     * and sweep the freq
     *
     * Example: 7032000C
     */
    case 'C':
    case 'c':
      freq_start = validate_input_freq(serial_input_number);
      frequency_sweep(freq_start);
      serial_input_number = 0L;
      endOfCommand();
      break;

    /*****************************************
     * Calibration rutine
     *
     * Example: 400F when you are
     *               using 400 ohm dummy load
     *****************************************/
    case 'F':
    case 'f':
       full_band_calibration(validate_input_freq(serial_input_number));
       serial_input_number = 0L;
       endOfCommand();
       break;

    /***********************************
     * Set number of steps in the sweep
     *
     * Example: 100N
     */
    case 'N':
    case 'n':
      num_steps = (int)serial_input_number;
      serial_input_number = 0L;
      endOfCommand();
      break;

    /**************
     * Start Sweep
     */
    case 'S':
    case 's':
      perform_sweep();
      endOfCommand();
      break;

    case 'R':
    case 'r':
      calc_analog_input_offsets();
      endOfCommand();
      break;

    /*************
     * Print Info
     */
    case '?':
      sweep_info();
      endOfCommand();
      break;

    /****************
     * Print Help
     */
    case 'H':
    case 'h':
      help();
      endOfCommand();
      break;

    /***************
     * Print Version
     */
    case 'V':
    case 'v':
      version();
      endOfCommand();
      break;
  }
}

/************************************************
 * Validates input frequency
 *
 * Allowed frequencies are between 1Hz and 30MHz
 *
 * Input: input_freq - input frequency
 *
 * Return: a frequency from valid interval
 ************************************************/
unsigned long validate_input_freq( unsigned long input_freq )
{
  if ( input_freq <= 0 )
  {
    return 1;
  }

  if ( input_freq > 30.0E6 )
  {
    return 30.0E6;
  }

  return input_freq;
}

/***************
 * Print info
 ***************/
void sweep_info( void )
{
  Serial.println("--- Sweep info ---");
  Serial.print("Start Freq:\t\t");
  Serial.println(freq_start);
  Serial.print("Stop Freq:\t\t");
  Serial.println(freq_stop);
  Serial.print("Num Steps:\t\t");
  Serial.println(num_steps);
  Serial.print("Analog Offset FWD:\t");
  Serial.println(offset_forward);
  Serial.print("Analog Offset REV:\t");
  Serial.println(offset_reverse);
}

/***************************
 * Print current build info
 ***************************/
void version( void )
{
  Serial.println(SoftwareVersion);
}

/**************
 * End Command
 **************/
 void endOfCommand( void )
 {
  Serial.println("#OK#");
  Serial.flush();
 }

/**************
 * Print Help
 **************/
void help( void )
{
  Serial.println("---- Commands may be lower or upper case ----");
  Serial.println("letter\tdescription\t\texample\t\tresult");
  Serial.println("'a'\tset start frequency\t6000000a\t6.000 Mhz");
  Serial.println("'b'\tset end frequency\t8000000b\t8.000 Mhz");
  Serial.println("'c'\tsweep const. frequency\t7032000c\t7.032 Mhz");
  Serial.println("'f'\tcalibration \t\t300f\t\tusing 300ohm dummy load");
  Serial.println("'n'\tset number of steps\t100n\t\t100 steps");
  Serial.println("'s'\tperform sweep from frequency a to b with n steps");
  Serial.println("'r'\tre-calibration of analog inputs");
  Serial.println("'?'\tsweep info");
  Serial.println("'h'\thelp");
  Serial.println("'v'\tsoftware version");

}

/**************************************************************************
 * Calibration Rutine
 *
 * The function sends output numbers in following format via Serial Port
 *
 * <EXPECTED_SWR, FREQUENCY_HZ, RAW_SWR>
 * <EXPECTED_SWR, FREQUENCY_HZ, RAW_SWR>
 * <EXPECTED_SWR, FREQUENCY_HZ, RAW_SWR>
 * .....
 * <EXPECTED_SWR, FREQUENCY_HZ, RAW_SWR>
 *
 * Where EXPECTED_SWR = resistor / 50
 *
 * Input: resistor - dummy load in Ohm
 *
 * Return: none
 **************************************************************************/
void full_band_calibration( unsigned int resistor )
{
  unsigned long curr_freq = 0L;

  /* it will measure SWR for every 1MHz interval.
   * SWR is measeured in the middle of intervals
   */
  for ( curr_freq = 0.5E6; curr_freq <= 30.0E6; curr_freq = curr_freq + 1.0E6 )
  {
    Serial.print((float)(resistor)/ 50.0, 2);
    Serial.print(", ");
    frequency_sweep(curr_freq);
  }
}

/****************************************************************************
 * Calculates Analog Input Offset
 *
 * New offset are stored in global variable
 * The function sends output numbers in following format via Serial Port
 * <FWD, REV>
 * ....
 ***************************************************************************/
void calc_analog_input_offsets( void )
{
  int samples_rev[CALIBRATION_SAMPLE_SZ];
  int samples_for[CALIBRATION_SAMPLE_SZ];

  // Switch off DDS for an analog input calibration
  dds_off();
  delay(100);
  dds_off();  // LF: I don't know why it is called two times - from original code
  delay(100);

  offset_reverse = 0;
  offset_forward = 0;

  // get samples from Analog Input
  for( byte i = 0; i < CALIBRATION_SAMPLE_SZ; i++ )
  {
    samples_for[i] = analogRead(A1);
    samples_rev[i] = analogRead(A0);
    Serial.print(samples_for[i]);
    Serial.print(", ");
    Serial.println(samples_rev[i]);
    delay(100);
  }

  // compute Median from the values
  offset_reverse =  median(CALIBRATION_SAMPLE_SZ, samples_rev);
  offset_forward =  median(CALIBRATION_SAMPLE_SZ, samples_for);
}

/***********************************************************************
 * it measures and computes SWR
 *
 * RAW SWR is computed as
 *
 *           (FWD + REV)
 * RAW_SWR = ------------
 *           (FWD - REV)
 *
 * calibrated SWR = coef3 * RAW_SWR^3 + coef2 * RAW_SWR^2 + coef1 * RAW_SWR + con
 *
 * coefs3, coefs2, coefs1, con depend on frequency.
 *
 * Input:
 * freq = input frequency
 * fwd = output parameter - raw FWD value
 * rev = output parameter - raw REV value
 * raw_swr = output parameter - raw SWR value
 *
 * Return: calibrated SWR
 *
 ***********************************************************************/
float measure_swr( unsigned long freq, int *fwd, int *rev, float *raw_swr )
{
  float swr = 0.0;
  byte coef_index = 0;

  *raw_swr = 0.0;

  // Read the forawrd and reverse voltages
  *rev = analogRead(A0) - offset_reverse;
  *fwd = analogRead(A1) - offset_forward;

  if ( *rev >= *fwd )
  {
    *rev = *fwd - 1;
  }

  if ( *rev < 1 )
  {
    *rev = 1;
  }

  //Compute the SWR from input values
  *raw_swr = (float)(*fwd + *rev) / (float)(*fwd - *rev);

  // If SWR is too high then fix a value
  if ( *raw_swr >= 29.0 )
  {
    *raw_swr = 28.999;
  }

  // If SWR is negative then assigne value 1
  if ( *raw_swr < 1 )
  {
    *raw_swr = 1;
  }

  // If it is too low then it means SWR 1:1
  if ( *raw_swr < 1.01 )
  {
    return 1.0;
  }

  // Compute Frequency interval index
  coef_index = (byte)(freq / 1.0E6);

  // Should function do a correction for particular freq interval?
  if ( calib_coefs[coef_index].do_correction == 1 )
  {
    swr =   calib_coefs[coef_index].coef3 * pow(*raw_swr, 3)
          + calib_coefs[coef_index].coef2 * pow(*raw_swr, 2)
          + calib_coefs[coef_index].coef1 * (*raw_swr)
          + calib_coefs[coef_index].con;
  }
  else
  {
    swr = *raw_swr;
  }

  // If final SWR is too high then fix a value
  if ( swr >= 29.0 )
  {
    swr = 28.999;
  }

  return swr;
}

/************************************************************************
 * Sweeping one frequency.
 * it takes samples of RAW SWR and computes a final SWR as a median of RAW SWR samples
 *
 * The function sends output numbers in following format via Serial Port:
 *
 * <FREQ, MEDIA(RAW_SWRs)>
 *
 * Input:  freq - frequence
 ***********************************************************************/
void frequency_sweep( unsigned long freq )
{
  int fwd_analog_value = 0;
  int rev_analog_value = 0;
  float swr_samples[CALIBRATION_SAMPLE_SZ];
  float result_swr = 0.0;

  // Set DDS to current frequency
  SetDDSFreq(freq);

  for ( byte i = 0 ; i < CALIBRATION_SAMPLE_SZ; i++ )
  {
     /* !!!! we uses only a raw value of SWR for calibration !!!!*/
     measure_swr(freq, &fwd_analog_value, &rev_analog_value, swr_samples + i);
     delay(100);
  }

  dds_off();

  // Determine Media from the values
  result_swr =  median_float(CALIBRATION_SAMPLE_SZ, swr_samples);

  // Send current line back to PC over serial bus
  // Format
  // Frequency, VSWR, FWD, REV
  Serial.print(freq);
  Serial.print(", ");
  Serial.println(result_swr, 5);
  Serial.flush();
}

/************************************************************************
 * Sweeping frequency range
 *
 * The function sends output numbers in following format via Serial Port
 *
 * <FREQ, SWR, ANALOG_FWD, ANALOG_REV, RAW_SWR>
 *
 ************************************************************************/
void perform_sweep( void )
{
  int fwd_analog_value = 0;
  int rev_analog_value = 0;
  float raw_swr = 0.0;
  unsigned long curr_freq = 0L;
  unsigned long freq_step = (freq_stop - freq_start) / num_steps;


  for ( unsigned int i = 0; i <= num_steps; i++ )
  {
    // Calculate current frequency
    curr_freq = freq_start + (i * freq_step);

    // Set DDS to current frequency
    SetDDSFreq(curr_freq);

    // Send current line back to PC over serial bus
    // Format
    // Frequency, VSWR, FWD, REV

    Serial.print(curr_freq);
    Serial.print(", ");
    Serial.print(measure_swr(curr_freq, &fwd_analog_value, &rev_analog_value, &raw_swr), 2);
    Serial.print(", ");
    Serial.print(fwd_analog_value);
    Serial.print(", ");
    Serial.print(rev_analog_value);
    Serial.print(", ");
    Serial.println(raw_swr);
  }

  dds_off();

  Serial.flush();
}

/*****************************
 * Set DDS Frequency
 *
 * Input: freq  - freq in HZ
 ****************************/
void SetDDSFreq( unsigned long freq )
{
  // Calculate the DDS word - from AD9850 Datasheet
  int32_t f = freq * 4294967295 / 125000000;

  // Send one byte at a time
  for (byte b = 0; b < 4; b++, f >>= 8)
  {
    send_byte(f & 0xFF);
  }
  // final control byte. 0 for 9850
  send_byte(0);

  // Done. Inform DDS
  pulseHigh(FQ_UD);

  delay(200);
}

/*******************
 * Send Byte to DDS
 ******************/
void send_byte( byte data_to_send )
{
  // Bit bang the byte over the SPI bus
  for (byte i = 0; i < 8; i++, data_to_send >>= 1)
  {
    // Set Data bit on output pin
    digitalWrite(SDAT, data_to_send & 0x01);

    // Send clock afte each bit.
    pulseHigh(SCLK);
  }
}

/*****************
 * Switch Off DDS
 *****************/
void dds_off( void )
{
  SetDDSFreq(0L);
}

/****************
 * Compute median
 ***************/
int median(int n, int x[])
{
  int temp;
  byte i, j;

  for( i = 0; i < n - 1; i++ )
  {
    for( j= i + 1; j < n; j++ )
    {
       if( x[j] < x[i] )
       {
         temp = x[i];
         x[i] = x[j];
         x[j] = temp;
       }
     }
  }

  if( n%2 == 0 )
  {
    return((x[n/2] + x[n/2 - 1]) / 2);
  }

  return x[n/2];
}

/****************
 * Compute median
 ***************/
float median_float(int n, float x[])
{
  float temp;
  byte i, j;

  for( i = 0; i < n - 1; i++ )
  {
    for( j= i + 1; j < n; j++ )
    {
       if( x[j] < x[i] )
       {
         temp = x[i];
         x[i] = x[j];
         x[j] = temp;
       }
     }
  }

  if( n%2 == 0 )
  {
    return((x[n/2] + x[n/2 - 1]) / 2.0);
  }

  return x[n/2];
}
