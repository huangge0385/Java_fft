 /*
  *  Copyright 2006-2007 Columbia University.
  *
  *  This file is part of MEAPsoft.
  *
  *  MEAPsoft is free software; you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License version 2 as
  *  published by the Free Software Foundation.
  *
  *  MEAPsoft is distributed in the hope that it will be useful, but
  *  WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  *  General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with MEAPsoft; if not, write to the Free Software
  *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  *  02110-1301 USA
  *
  *  See the file "COPYING" for the text of the license.
  */
 
 package fft;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
 
 
 public class FFT {
 
   int n, m;
   
   // Lookup tables.  Only need to recompute when size of FFT changes.
   double[] cos;
   double[] sin;
   double[] window;
   
   public FFT(int n) {
     this.n = n;
     this.m = (int)(Math.log(n) / Math.log(2));
 
     // Make sure n is a power of 2
     if(n != (1<<m))
       throw new RuntimeException("FFT length must be power of 2");
 
     cos = new double[n/2];
     sin = new double[n/2];
 

 
     for(int i=0; i<n/2; i++) {
       cos[i] = Math.cos(-2*Math.PI*i/n);
       sin[i] = Math.sin(-2*Math.PI*i/n);
     }
 
     makeWindow();
   }
 
   protected void makeWindow() {
     // Make a blackman window:
     // w(n)=0.42-0.5cos{(2*PI*n)/(N-1)}+0.08cos{(4*PI*n)/(N-1)};
     window = new double[n];
     for(int i = 0; i < window.length; i++)
       window[i] = 0.42 - 0.5 * Math.cos(2*Math.PI*i/(n-1)) 
         + 0.08 * Math.cos(4*Math.PI*i/(n-1));
   }
   
   public double[] getWindow() {
     return window;
   }
 
 
   /***************************************************************
   * fft.c
   * Douglas L. Jones 
   * University of Illinois at Urbana-Champaign 
   * January 19, 1992 
   * http://cnx.rice.edu/content/m12016/latest/
   * 
   *   fft: in-place radix-2 DIT DFT of a complex input 
   * 
   *   input: 
   * n: length of FFT: must be a power of two 
   * m: n = 2**m 
   *   input/output 
   * x: double array of length n with real part of data 
   * y: double array of length n with imag part of data 
   * 
   *   Permission to copy and use this program is granted 
   *   as long as this header is included. 
   ****************************************************************/
   public void fft(double[] x, double[] y)
   {
     int i,j,k,n1,n2,a;
     double c,s,e,t1,t2;
   
   
     // Bit-reverse
     j = 0;
     n2 = n/2;
     for (i=1; i < n - 1; i++) {
       n1 = n2;
       while ( j >= n1 ) {
         j = j - n1;
         n1 = n1/2;
       }
       j = j + n1;
     
       if (i < j) {
         t1 = x[i];
         x[i] = x[j];
         x[j] = t1;
         t1 = y[i];
         y[i] = y[j];
         y[j] = t1;
       }
     }
 
     // FFT
     n1 = 0;
     n2 = 1;
   
     for (i=0; i < m; i++) {
       n1 = n2;
       n2 = n2 + n2;
       a = 0;
     
       for (j=0; j < n1; j++) {
         c = cos[a];
         s = sin[a];
         a +=  1 << (m-i-1);
 
         for (k=j; k < n; k=k+n2) {
           t1 = c*x[k+n1] - s*y[k+n1];
           t2 = s*x[k+n1] + c*y[k+n1];
           x[k+n1] = x[k] - t1;
           y[k+n1] = y[k] - t2;
           x[k] = x[k] + t1;
           y[k] = y[k] + t2;
         }
       }
     }
   }                          
 
   // Test the FFT to make sure it's working
   public static void main(String[] args) throws IOException {
     
	   InputStream myxls = new FileInputStream("/Users/huangge/Documents/workspace/fft/src/BxDec99.xls");		
	   HSSFWorkbook wb     = new HSSFWorkbook(myxls);
	   HSSFSheet sheet = wb.getSheetAt(0);  
	   int rowStart = Math.min(15, sheet.getFirstRowNum());
	   int rowEnd = Math.max(1400, sheet.getLastRowNum());
	   Row r_for_rowCount = sheet.getRow(0);
	   int lastColumn = Math.min(r_for_rowCount.getLastCellNum(), 1000);
	    
	   double[][] res = new double[lastColumn-1][rowEnd ];
	   Workbook wb_out = new HSSFWorkbook();  // or new XSSFWorkbook();
	   Sheet sheet_out = wb_out.createSheet();
	   int count = 0;
	   for (int j = 1; j < lastColumn; j++) {//make res matrix
		   count = 0;
		   for (int i = 1; i <= rowEnd ; i++) {
			   Row r = sheet.getRow(i);
			   Cell c = r.getCell(3, Row.RETURN_BLANK_AS_NULL);
			   if (c == null || c.getCellType() == Cell.CELL_TYPE_BLANK) {
				  break;
				 }
			   else if (c.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				   res[j-1][i-1] = c.getNumericCellValue();
				   count++;
			   }
		   }
	   }
	   
	   int N = count ;
	   int nextPowTwo = 1;
	   while (nextPowTwo < N) {
			nextPowTwo += nextPowTwo;
		}
	   N = nextPowTwo;
	   FFT fft = new FFT(N);
	   double[] window = fft.getWindow();
	   double[] re = new double[N];
	   Arrays.fill(re, 0);;
	   double[] im = new double[N];
	   
	   
	   for (int i = 0; i < re.length/2; i++) {//initial sheet
		 Row row_cre = sheet_out.createRow(i);
   		for (int k = 0; k < lastColumn-1; k++) {
   			Cell cell   = row_cre.createCell((short)(k)); 
			}
	    }
	   	
		  for (int j = 1; j < lastColumn; j++) {//make result sheet
			    for(int i=0; i<count ; i++)
			       {re[i] = res[j-1][i]; 
			       im[i] = 0;}
			     beforeAfter(fft, re, im);
			    for (int i = 0; i < re.length/2; i++) {
			    	Row row_out     = sheet_out.getRow(i); 
			    	Cell cell   = row_out.getCell((short)(j-1)); 
			    	cell.setCellValue(Math.abs(re[i])); 
				}
		 
		}  
		
	   FileOutputStream fileOut//write file
	    = new FileOutputStream("/Users/huangge/Documents/workspace/fft/src/workbook.xls");
	    wb_out.write(fileOut);
	    fileOut.close();

 
     long time = System.currentTimeMillis();
     double iter = 10;
     for(int i=0; i<iter; i++)
      // fft.fft(re,im);
     time = System.currentTimeMillis() - time;
     System.out.println("Averaged " + (time/iter) + "ms per iteration");
   }
 
   protected static void beforeAfter(FFT fft, double[] re, double[] im) {
     System.out.println("Before: ");
     printReIm(re, im);
     fft.fft(re, im);
     System.out.println("After: ");
     printReIm(re, im);
   }
 
   protected static void printReIm(double[] re, double[] im) {
     System.out.print("Re: [");
     for(int i=0; i<re.length; i++)
       System.out.print(((int)(re[i]*1000)/1000.0) + " ");
 
     System.out.print("]\nIm: [");
     for(int i=0; i<im.length; i++)
       System.out.print(((int)(im[i]*1000)/1000.0) + " ");
 
     System.out.println("]");
   }
 }