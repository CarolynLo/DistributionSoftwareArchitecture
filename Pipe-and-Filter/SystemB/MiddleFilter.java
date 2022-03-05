import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static java.lang.Long.sum;

/******************************************************************************************************************
* File:MiddleFilter.java
* Project: Lab 1
* Copyright:
*   Copyright (c) 2020 University of California, Irvine
*   Copyright (c) 2003 Carnegie Mellon University
* Versions:
*   1.1 January 2020 - Revision for SWE 264P: Distributed Software Architecture, Winter 2020, UC Irvine.
*   1.0 November 2008 - Sample Pipe and Filter code (ajl).
*
* Description:
* This class serves as an example for how to use the FilterRemplate to create a standard filter. This particular
* example is a simple "pass-through" filter that reads data from the filter's input port and writes data out the
* filter's output port.
* Parameters: None
* Internal Methods: None
******************************************************************************************************************/

public class MiddleFilter extends FilterFramework
{
	public void run()
    {
		// Data format
		Calendar TimeStamp = Calendar.getInstance();
		SimpleDateFormat TimeStampFormat = new SimpleDateFormat("yyyy:MM:dd:hh:mm:ss");
		NumberFormat formatter = new DecimalFormat("#.00000"); // Number format

		int bytesread = 0;					// Number of bytes read from the input file.
		int byteswritten = 0;				// Number of bytes written to the stream.
		byte databyte = 0;					// The byte of data read from the file
		String results="Time, Velocity, Altitude, Pressure, Temperature\n"; // For the final output result, with origin data
		double lastAlt=0, dataAlt=0, diff=0;
		long measurement=0, average=0, measurementLast=0, measurementLastTwo=0;
		int id=0; // Id for recognizing measurements
		String wildPoint="";
		boolean replace=false;

		// Next we write a message to the terminal to let the world know we are alive...
		System.out.println( "\n" + this.getName() + "::Middle Reading ");

		// for record one, ( ID 0~4 )
		for (int k=0; k<5; k++ )
		{
			try {
				id = 0;
				for (int i=0; i<4;i++ )
				// An ID is an integer, so idLength is 4 bytes
				{
					databyte = ReadFilterInputPort();
					id = id | (databyte & 0xFF);
					if (i != 3)
					{
						id = id << 8;
						// one byte = 8 bits, so left shift 8 bits here.
					}
					bytesread++;
					// Write ID after reading
					WriteFilterOutputPort(databyte);
					byteswritten++;
				}
				measurement = 0;
				for (int j=0; j<8; j++ )
				// A measurement is a double (8 bytes), so MeasurementLength is 8
				{
					databyte = ReadFilterInputPort();
					measurement = measurement | (databyte & 0xFF);
					if (j!=7)
					{
						measurement = measurement << 8;
						// measurement
						// Shift 8 bits = 1 byte, need to move and space
					}
					bytesread++;
					// write directly for record one
					WriteFilterOutputPort(databyte);
					byteswritten++;
				}
				if(id==2){
					measurementLastTwo=measurement;
					measurementLast=measurement;
//					lastAlt=Double.longBitsToDouble(measurement);
//					lastSecondAlt=lastAlt;
//					System.out.println(lastAlt);
				}
			} catch (EndOfStreamException e) {
				e.printStackTrace();
			}
		}
		while (true)
		{
			// Here we read a byte and write a byte
			replace = false;
			try {
				// For each record from ID 0~4
				for (int k = 0; k < 5; k++) {

					id = 0;
					for (int i = 0; i < 4; i++)
					// An ID is an integer, so idLength is 4 bytes
					{
						databyte = ReadFilterInputPort();
						id = id | (databyte & 0xFF);
						if (i != 3) {
							id = id << 8;
							// one byte = 8 bits, so left shift 8 bits here.
						}
						bytesread++;
						// Write ID after reading
						WriteFilterOutputPort(databyte);
						byteswritten++;
					}
					measurement = 0;
					for (int j = 0; j < 8; j++)
					// A measurement is a double (8 bytes), so MeasurementLength is 8
					{
						databyte = ReadFilterInputPort();
						measurement = measurement | (databyte & 0xFF);
						if (j != 7) {
							measurement = measurement << 8;
							// Shift 8 bits = 1 byte, need to move and space
						}
						bytesread++;
					}

					// Save original data in WildPoint.csv
					if ( id == 0 )
					{
						TimeStamp.setTimeInMillis(measurement);
					}
					// Print out ID and the other four measurement with specific format.
					if(id == 4){
						wildPoint+=formatter.format( Double.longBitsToDouble(measurement))+"\n";
					}else if(id != 0){
						wildPoint+=formatter.format( Double.longBitsToDouble(measurement))+", ";
					}else{
						wildPoint+=TimeStampFormat.format(TimeStamp.getTime())+", ";
					}





					if (id == 2) {
						dataAlt = Double.longBitsToDouble(measurement);
						lastAlt=Double.longBitsToDouble(measurementLast);
						diff=dataAlt-lastAlt;
						average=sum(measurementLast>>1,measurementLastTwo>>1);
						if(diff > 100 || diff < -100) {
							replace=true;
							for (int p = 0; p < 8; p++) {
//								measurement = average;
								WriteFilterOutputPort(longToBytes(average)[p]);
								byteswritten++;
							}
							// If replace write ID=7 to SinkFilter.java, to add * on the output file
							WriteFilterOutputPort((byte) 0);
							byteswritten++;
							WriteFilterOutputPort((byte) 0);
							byteswritten++;
							WriteFilterOutputPort((byte) 0);
							byteswritten++;
							WriteFilterOutputPort((byte) 7);
							byteswritten++;
						}
						else{
							// Write for each byte in a measurements
							for (int p = 0; p < 8; p++) {
								WriteFilterOutputPort(longToBytes(measurement)[p]);
								byteswritten++;
							}
						}

						// Change the last and the second last record
						measurementLastTwo=measurementLast;
						measurementLast=measurement;
//						newRecord = sum(measurement >> 1, measurement >> 1);
					}else{
						// Write for each byte in a measurements
						for (int p = 0; p < 8; p++) {
							WriteFilterOutputPort(longToBytes(measurement)[p]);
							byteswritten++;
						}
					}
				} // for

				// Only save wild jump records
				if(replace){
					results+=wildPoint;
				}
				wildPoint="";
			} // try
			catch (EndOfStreamException e)
			{
				ClosePorts();
				System.out.println( "\n" + this.getName() + "::Middle Exiting; bytes read: " + bytesread + " bytes written: " + byteswritten );
				break;
			}
		} // while

		// Write to the output file "WildPoints.csv"
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("WildPoints.csv"));
			writer.write(results);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
   } // run

	private static ByteBuffer buffer = ByteBuffer.allocate(8);
	public static byte[] longToBytes( long x ){
		buffer.putLong(0,x);
		return buffer.array();
}
}
/*
Id -> (id!=2) measurement, send to Sink directly

if(id!=2){
	read -> write
		}

if(id==2){
	if( first record){
	}else{
		wildJump=true or not.
	}
	send measurement to Sink, double - > long -> byte[]
	if(wildJump){
	send "signal" (4bytes, value= 7)  to Sink, // Sink read bytes, when it read "signal"
	}

}
 */