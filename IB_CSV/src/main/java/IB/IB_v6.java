package IB;

//v1- Plots IndexBands with color set in UI. Uses HistoricalDataListener.
//v2- Same as v1 except that it............. DOES NOT Use HistoricalDataListener.
//v3- Had to delete v3. KEpt getting failed to enumerate entry points errors.
//v3- I took copy of v2 and v4 and named v3 and they still failed???
//v4- New color settings. Compiles and plots but new colors from CSV signal still Don't work.
//v5- copy of v4 for more testing. This had more complex color settings for later use after i figure out how to Double.NaN after change
//v6- copy of v2. Added Arrays to store previous values

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.JComboBox;
import javax.swing.JLabel;


import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1SimpleAttachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.simplified.Api;
import velox.api.layer1.simplified.CustomModule;
//import velox.api.layer1.simplified.HistoricalDataListener;
//import velox.api.layer1.simplified.HistoricalModeListener;
import velox.api.layer1.simplified.Indicator;
import velox.api.layer1.simplified.IndicatorModifiable;
import velox.api.layer1.simplified.InitialState;
import velox.api.layer1.simplified.IntervalListener;
import velox.api.layer1.simplified.Intervals;
import velox.api.layer1.simplified.LineStyle;
import velox.api.layer1.settings.StrategySettingsVersion;
import velox.api.layer1.simplified.TimeListener;
import velox.gui.StrategyPanel;
import velox.gui.colors.ColorsConfigItem;
import velox.api.layer1.simplified.CustomSettingsPanelProvider;
import com.bookmap.api.simple.demo.utils.gui.BookmapSettingsPanel;


@Layer1SimpleAttachable
@Layer1StrategyName("IB_v6")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IB_v6 implements CustomModule, IntervalListener/*, HistoricalDataListener*/, TimeListener, CustomSettingsPanelProvider {

    
     @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})	  	
     public static class Settings {
    	 public int cUpdateInt = defaultUpdateInterval;
	     public Color colorBand =  defBandc;
	     public Color colorMBand = defMBandc;
	  
	     public int lineBWidth = defBLineWidth;
	     public int lineMBWidth = defMBLineWidth;      
	     	     
	     public LineStyle lineBStyle = defBLineStyle;
	     public LineStyle lineMBStyle = defMBLineStyle;	     
		      
	     public boolean reloadOnChange = true;
    } 
    
	protected Settings settings;
    protected Api api;
	
	protected IndicatorModifiable BandH;
	protected IndicatorModifiable BandL;
	protected IndicatorModifiable MBandH;
	protected IndicatorModifiable MBandL;		
 	
	private int minimalIntervalCount;
    final long minimalInterval= Intervals.INTERVAL_15_SECONDS;  
    private long nCustomInterval;
	private static final int defaultUpdateInterval = 30;	
    private static final Color defBandc =  Color.GREEN;
    private static final Color defMBandc = Color.GREEN.darker();//.darker();
    
    private static final int defBLineWidth = 4;     
    private static final int defMBLineWidth = 4;       
    
    private static final LineStyle defBLineStyle = LineStyle.SOLID;
    private static final LineStyle defMBLineStyle = LineStyle.DOT;    
    

    Double pipV  = 0.0;
    private long t;
 	protected String TimeStr; 
 	 
 	private Double Price = null;	
 	//private Boolean newPlotprice; 
 			
 	private volatile String symStr = ""; // should this be static and volatile?
 	private volatile String newStr = "";			    // should this be private only?
 	private volatile String finalfilepath;				// should this be private only?

 	private int[] bTimeArray = new int[3];
	private int[] mbTimeArray = new int[3];

		
	 private void setBVisualProperties(final Indicator indicator) {
	   indicator.setLineStyle(settings.lineBStyle);
	   indicator.setWidth(settings.lineBWidth);    
	 }
		
	 private void setMBVisualProperties(final Indicator indicator) {
	   indicator.setLineStyle(settings.lineMBStyle);
	   indicator.setWidth(settings.lineMBWidth);          
	 }
	
	 
    @Override
    public void initialize(String alias, InstrumentInfo info, Api api, InitialState initialState) {
 		Log.info("Initialize " + symStr);
		this.api = api;
        settings = api.getSettings(Settings.class);		      
        nCustomInterval = settings.cUpdateInt;
        
        pipV = info.pips;       
 		
        this.symStr = info.symbol.toString();					//added 'this.'
		this.newStr = symStr.substring(0, symStr.length() - 2); //added 'this.' which seems to fix newStr from ging string value from second subscription 
		 		
		this.finalfilepath = "C:\\Bookmap\\Notes\\BookMap_IB_ADDon_" + newStr + "(RITHMIC).csv"; 	// How do i chk if file exists?				 
        
		this.pipV = info.pips;
		
		this.BandH = api.registerIndicatorModifiable("BandH",GraphType.PRIMARY);//added 'this.'
	    setBVisualProperties(BandH);											 //should i add 'this.' to these also?
	    BandH.setColor(defBandc);											 //should i add 'this.' to these also?
		
		this.BandL = api.registerIndicatorModifiable("BandL",GraphType.PRIMARY);//added 'this.'
	    setBVisualProperties(BandL);											 //should i add 'this.' to these also?
	    BandL.setColor(defBandc);		
	    
		this.MBandH = api.registerIndicatorModifiable("mBandH",GraphType.PRIMARY);//added 'this.'
	    setMBVisualProperties(MBandH);											 //should i add 'this.' to these also?
	    MBandH.setColor(defMBandc);											 //should i add 'this.' to these also?
		
		this.MBandL = api.registerIndicatorModifiable("mBandL",GraphType.PRIMARY);//added 'this.'
	    setMBVisualProperties(MBandL);											 //should i add 'this.' to these also?
	    MBandL.setColor(defMBandc);	   
	    	    
	    readCsv(finalfilepath) ;	
   }
 
    /*static*/ void readCsv(String nfilePath) {
		BufferedReader reader = null;
		
		  try {			  
			  //.info("readCSV " + symStr + " " + filePath);
			  List<PlotVals> plotV = new ArrayList<PlotVals>();
			  String line = "";
			  reader = new BufferedReader(new FileReader(nfilePath));
			  reader.readLine();
		   
			 while((line = reader.readLine()) != null) {
				 String[] fields = line.split(",");
				  
				  if(fields.length > 0) {					 
					  	PlotVals plotT = new PlotVals();
					  	plotT.setSymbol(fields[0]);					  	
					  	plotT.setPLevel(fields[1]);
					  	plotT.setPName(fields[2]);					  	
					  	plotT.setStartTime(fields[3]);
					  	plotT.setPTrend(fields[4]);	  
				 		 
					  	plotV.add(plotT);
					  	//.info("GotCSVDATA");
				  }
			 }
			 
			for(PlotVals p: plotV) {		
				//Log.info("ReadCSV- " + symStr +  " = " + p.getSymbol() + " PLevel " +  p.getPLevel() + " Note " + p.getPName() + " StartTime " + p.getStartTime() + " PTrend " + p.getPTrend());
				compiledata(p.getPName(),p.getPLevel(), p.getStartTime(),p.getPTrend() );
				
				//Log.info("ReadCSV- Compile Finished");
				//System.out.printf("[symbol=%s, pricelevel=%s, note=%s, fcolor=%s, bcolor=%s]\n", p.getSymbol(), p.getPLevel(), p.getNote(), p.getFColor(), p.getBColor());
			}		   
		  } 
		  catch (Exception ex) {
		 	  Log.info("Exception ex1 |" + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");			  
			  ex.printStackTrace();
		  } 
		  finally {
			  try {
				  reader.close();
				  //Log.info("finally- Finished Loop " + symStr);
				  //addPoints(localT);	//Is this ok here?
			  } 
			  catch (Exception e) {
				  Log.info("Exception ex2 |" + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
				  e.printStackTrace();			  
			  }
		  }		  
	}
    

    /*
   	 public static void main(String[] args){
   		 Log.info("MAin " + symStr); 
   		
   		 readCsv(finalfilepath) ;		  	
   	 }
   */

   	 @Override
   		public void onTimestamp(long t) {
   		   this.t = t; 		  
   		}
   	 
   	 
   	public LocalDateTime localT() {	
   		return LocalDateTime.ofInstant(Instant.ofEpochSecond(t / 1_000_000_000L), TimeZone.getDefault().toZoneId());	   	  
   	}
   	
   	
   	/*    
   	 @Override
   	 public void onBbo(int bidPrice, int bidSize, int askPrice, int askSize) {
   		   	   	
   	 }  
   	 */	
   		    
   	/*
   	 @Override
   	 public void onTrade(double price, int size, TradeInfo tradeInfo){
   	
   	 }   
   	 */
   	  
	 @Override
	 public long getInterval() {
	    // return Intervals.INTERVAL_15_SECONDS;// add parameter so u can set time updates
	     return minimalInterval;
	 }

	 @Override
	 public void onInterval() {
		//LocalDateTime tStamp = localT();
	 	long customInterval = (long) nCustomInterval * 1_000_000_000L;//seconds to nanos ?
	 	
	 	  if (customInterval != 0 //customInterval is valid
	                && customInterval % minimalInterval == 0 // customInterval is a multiple of minimalInterval 
	                && minimalIntervalCount % (customInterval/minimalInterval) == 0 // minimalIntervalCount is a multiple of custom/minimal ratio
	                ) {
	 		   	  
	 		  Log.info("++++++++++++++++++ Begin Loop Thru at onInterval+++++++++++++++++");			
	 		  //Log.info("onINterval- "  + symStr + " Time is "  + tStamp.toString());	
			
	 		  readCsv(finalfilepath) ;			 	 
	 		  //addPoints(localT);
	 	  }			
	      minimalIntervalCount++;	
	 }  

     public static String getStrings(String N) {
    	 String plotStr; 
 		if (N.indexOf("/") != -1 ) {
			plotStr = N.substring(0,N.indexOf("/")); 
		}else{
			plotStr = N.toString(); 
		}		 
 		//Log.info("getStrings " + plotStr);
    	return plotStr;
     }

	 public static LocalTime getTimes(String Stime) {// Should i add this anywhere inside this method?
/*		 Integer HourInt = null;	
		 Integer MinInt = null;

		if (Stime.length() == 3 ) {
			HourInt = Integer.parseInt(Stime.substring(0,1));
			MinInt = Integer.parseInt(Stime.substring(1,3)); 
			//Log.info("HourInt " + HourInt + " |MinInt " + MinInt);						
		}else if(Stime.length() == 4 ) {
			HourInt = Integer.parseInt(Stime.substring(0,2));
			MinInt = Integer.parseInt(Stime.substring(2,4)); 
			//Log.info("HourInt " + HourInt + " |MinInt " + MinInt);				
		}
		LocalTime TempTime = LocalTime.of(HourInt, MinInt, 00);		
*/
		LocalTime TempTime = LocalTime.parse(Stime);		
		LocalTime TimeS = TempTime.minusMinutes(1);
		
		//Log.info("getTiimes- " + " |TimeS " + TimeS.toString());				
		return TimeS;		
	 }		
	 

	 public  Boolean setPrevBtime(LocalTime Btime) {// Should i add this anywhere inside this method?
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer BandH = Btime.getHour();
		 Integer BandM = Btime.getMinute();		
		 Integer BandS = Btime.getSecond();			 
		 Boolean isNewBand = null;
		 		 
		 if(!this.bTimeArray.equals(null)) {
			 Integer prevBandH = this.bTimeArray[0] ;		 
			 Integer prevBandM = this.bTimeArray[1] ;	
			 Integer prevBandS = this.bTimeArray[2] ;			 
			 Log.info("setPrevBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());
				 			 
			if(Btime.compareTo(LocalTime.of(prevBandH, prevBandM, prevBandS)) != 0) {
				isNewBand = true;
			}else {
				isNewBand = false;				
			}
		 }
		 Log.info("isNewBand- " + isNewBand.toString());
		 ;
		 this.bTimeArray[0] = BandH;	
		 this.bTimeArray[1] = BandM;	   
		 this.bTimeArray[2] = BandS;			 
	    
		//Log.info("getTiimes- " + " |StopT " + StopT.toString() + " |StartT " + StartT.toString());				
		return isNewBand;		
	 }	
	 
	 public  Boolean setPrevMBtime(LocalTime MBtime) {// Should i add this anywhere inside this method?
		 //ArrayList<String> arlist = new ArrayList<String>( );
		 Integer BandH = MBtime.getHour();
		 Integer BandM = MBtime.getMinute();		
		 Integer BandS = MBtime.getSecond();				 
		 Boolean isNewBand = true;
		 		 
		 if(!this.mbTimeArray.equals(null)) {
			 Integer prevBandH = this.mbTimeArray[0] ;		 
			 Integer prevBandM = this.mbTimeArray[1] ;		
			 Integer prevBandS = this.mbTimeArray[2] ;				 
			 Log.info("setPrevMBtime |" + LocalTime.of(prevBandH, prevBandM, prevBandS).toString());			
			 
			 if(MBtime.compareTo(LocalTime.of(prevBandH, prevBandM, prevBandS)) != 0) {
				isNewBand = true;
			}else {
				isNewBand = false;				
			}
		 }
		 Log.info("isNewMBand- " + isNewBand.toString());
		
		 this.mbTimeArray[0] = BandH;	
		 this.mbTimeArray[1] = BandM;		   
		 this.mbTimeArray[2] = BandS;	
		 
		//Log.info("getTiimes- " + " |StopT " + StopT.toString() + " |StartT " + StartT.toString());				
		return isNewBand;		
	 }	
	  	 
     public void compiledata(String PName, String Plevel, String Starttime, String PTrend) throws Exception {		  	
    	String plotStr = getStrings(PName);
    	LocalTime TimeS = getTimes(Starttime);     	
		LocalDateTime TimeNow = localT(); 
		Boolean newBPlot = null;
		Boolean newMBPlot = null;
		
		//this.Price = Double.parseDouble(Plevel);		
		if (!Plevel.contains("999999")) {
			this.Price = Double.parseDouble(Plevel);		
		}else {
			this.Price = Double.NaN;		
		}
			
		//	int[] currentPrice 		
		//onlyOnce();
	

		if(PTrend.equals("TRUE") || PTrend.equals("FALSE")) {
			newBPlot = this.setPrevBtime(TimeS) ;		
		}else if(PTrend.equals("m_TRUE") || PTrend.equals("m_FALSE")) {
			newMBPlot = this.setPrevMBtime(TimeS)  ;
		}
			
		
		if(newBPlot != null ) {
	      Log.info(plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |newBPlot= " + newBPlot.toString()); 		
	     if(newBPlot.equals(true)) {
	    	  Log.info("TRUE " + newBPlot);
	      }else if(newBPlot.equals(false)) {
	    	  Log.info("False " + newBPlot);
	      }else {
	    	  Log.info("Doooo " + newBPlot);
	      }
		}
	     
		Log.info("compileData- "  + symStr + " Time is "  + TimeNow.toString() + " |Price " + Price.toString() + " |plotStr " + plotStr + " |TimeS " + TimeS.toString() + " |PTrend " + PTrend.toString());   		
		Log.info("compileData1- "  +plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |Bool= " + newBPlot + " |Bool= " + newBPlot+ " |MBool= " + newMBPlot ); 

    	  if(newBPlot != null) {
  			Log.info(plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |Bool= " + newBPlot); 		
			    		 
	    	if (newBPlot.equals(true) && (plotStr.equals("BHi") || plotStr.equals("BHi's")) )  { 	
			 	Log.info("DONT PLOT BandH |" + plotStr + " | " + newBPlot);  	    		
	    		this.BandH.addPoint(Double.NaN);
	    		//this.BandH.addPoint(this.Price/pipV);   

	    	}else if (plotStr.equals("BHi") || plotStr.equals("BHi's")){
	    		Log.info("OK to Plot BandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		this.BandH.addPoint(this.Price/pipV);   
		    }	
	   	
	    	if (newBPlot.equals(true) && (plotStr.equals("BLo") || plotStr.equals("BLo's")) ) { 	    		   	    		   
	    		this.BandL.addPoint(Double.NaN);  
	    		//this.BandL.addPoint(this.Price/pipV); 	    		
			 	Log.info("DONT PLOT BandL |" + plotStr);  
	    	}else if (plotStr.equals("BLo") || plotStr.equals("BLo's")){
	    		Log.info("OK to Plot BandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		this.BandL.addPoint(this.Price/pipV);   		    		
		    }	
		//}else {			
		//	throw new Exception("BTimes did not compile correctly and are null!");
		//}
    	  }
		
		if(newMBPlot != null) {
			Log.info(plotStr + " |Price= " + Price + " |TimeS= " + TimeS + " |Bool= " + newMBPlot); 		
    				
	    	if (newMBPlot.equals(true) && plotStr.equals("mBHi") ) { 	    		    		
	    		this.MBandH.addPoint(Double.NaN);
	   		 	//this.MBandH.addPoint(this.Price/pipV);  
			 	Log.info("DONT PLOT MBandH |" + plotStr);  
	    	}else if (plotStr.equals("mBHi")) {
	    		Log.info("OK to Plot MBandH |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		this.MBandH.addPoint(this.Price/pipV);   
		    }	
	    	    	
	    	if (newMBPlot.equals(true) && plotStr.equals("mBLo") ) { 	    		   	    		   
	    		this.MBandL.addPoint(Double.NaN);   	
	    		//this.MBandL.addPoint(this.Price/pipV);   		    		
			 	Log.info("DONT PLOT MBandL |" + plotStr);  
	    	}else if (plotStr.equals("mBLo")) {
	    		Log.info("OK to Plot MBandL |" + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		this.MBandL.addPoint(this.Price/pipV);   	
		    }	
		//}else {			
		//	throw new Exception("MBTimes did not compile correctly and are null!");
		//}
		}
	    Log.info("added POINTS!");   
	
	    //prevT.compareTo(Tm) = TimeS;	
	}
	 
	/* @Override
	 public void onBar(OrderBook orderBook, Bar bar) {
	  }   
	*/
	
     protected void onSettingsChange(Long UpdInt) {
		 	Log.info("onSettingsChange " + localT() + " |NewUpdateInterval= " + UpdInt.toString() + " seconds."); 
		 	
		 	//nCustomInterval = (long) defaultUpdateInterval * 1_000_000_000L;//seconds to nanos ?;
		 	//Log.infoFree(interval); 
    }

	    			
	 @Override
	 public StrategyPanel[] getCustomSettingsPanels() {
	     StrategyPanel p1 = getIntervalSettingsPanel();		 
	     StrategyPanel p2 = getBStyleSettingsPanel();
	     StrategyPanel p3 = getMBStyleSettingsPanel();	
	     return new StrategyPanel[] { p1, p2, p3};        
	 }
	 
	 private StrategyPanel getIntervalSettingsPanel() {
	     BookmapSettingsPanel panel = new BookmapSettingsPanel("Update Interval");
	     addIntervalSettings(panel);

	     return panel;
	 }	
	 
	 private StrategyPanel getBStyleSettingsPanel() {
	     BookmapSettingsPanel panel = new BookmapSettingsPanel("Band settings");
	     addBLineStyleSettings(panel);
	     addBLineWidthSettings(panel);     
	     addBColorsSettings(panel);
	     return panel;
	 }	 
	 
	private StrategyPanel getMBStyleSettingsPanel() {
		 BookmapSettingsPanel panel = new BookmapSettingsPanel("mBAnd settings");
		 addMBLineStyleSettings(panel);
		 addMBLineWidthSettings(panel);	 
		 addMBColorsSettings(panel);
		 return panel;
	}	  
	
	private void addIntervalSettings(final BookmapSettingsPanel panel) {
		   JComboBox<Integer> c = new JComboBox<>(new Integer[] { 15, 30, 45, 60, 120, 180, 240, 300});
		   setAlignment(c);
		   int selected = (int) (settings.cUpdateInt);
		   c.setSelectedItem(selected);
		   c.setEditable(false);
		   c.addActionListener(new ActionListener() {
		
		 // c.setSelectedItem(settings.lineWidth);
		 
		       @Override
		       public void actionPerformed(ActionEvent e) {                            	               
		           int newUpdateInt = (int) c.getSelectedItem();
		           if (newUpdateInt != settings.cUpdateInt) {
		        	   settings.cUpdateInt = newUpdateInt;	                          
		               nCustomInterval = newUpdateInt;  
		               onSettingsChange(nCustomInterval);      	                   
		           }
		       };
		   });	       
		   panel.addSettingsItem("Interval (seconds):", c);
		}
	
	
	 private void addBColorsSettings(final BookmapSettingsPanel panel) {   
		 //Add For loop for these
	        panel.addSettingsItem("Band color:", createColorsConfigItem(1));		
	                  
	 }
	
	 private void addMBColorsSettings(final BookmapSettingsPanel panel) {   
		 //Add For loop for these  
	        panel.addSettingsItem("mBand color:", createColorsConfigItem(2));	 
	 }
	 	 
	private ColorsConfigItem createColorsConfigItem(Integer x) {
	    Consumer<Color> c = new Consumer<Color>() {
	       @Override
	       public void accept(Color color) {    
	    	   
	           if (x == 1) {
	               settings.colorBand = color;
	               BandH.setColor(settings.colorBand);
	               BandL.setColor(settings.colorBand);	               

	           } else if(x == 2){
	               settings.colorMBand = color;
	               MBandH.setColor(settings.colorMBand);
	               MBandL.setColor(settings.colorMBand);	               
	           }   	
	       }	
	   };
	   
	   Color color = (x == 1) ? settings.colorBand 
		   	   		: (x == 2) ? settings.colorMBand 		   	   		
				   			: Color.GRAY;//Is this correct way to finish off else statement
	 
	   
 
	   Color ndefaultColor = (x == 1) ? defBandc
				   	   		: (x == 2) ? defMBandc 		   	   		 									   									   	   		   								
						   			: Color.GRAY;//Why do i need this at end
	
		   //Log.info("TEST  " + x + " | " + color + " | " + ndefaultColor);
		   return new ColorsConfigItem(color, ndefaultColor, c);
		}
	
	 private void addBLineStyleSettings(final BookmapSettingsPanel panel) {
	     String[] lineStyles = Stream.of(LineStyle.values()).map(Object::toString).toArray(String[]::new);
	     JComboBox<String> c = new JComboBox<>(lineStyles);
	     c.setSelectedItem(settings.lineBStyle.toString());
	     c.setEditable(false);
	     c.addActionListener(new ActionListener() {
	         @Override
	         public void actionPerformed(ActionEvent e) {
	             int idx = c.getSelectedIndex();
	             if (idx != settings.lineBStyle.ordinal()) {
	                 settings.lineBStyle = LineStyle.values()[idx];
	                 BandH.setLineStyle(settings.lineBStyle);	                            
	                 BandL.setLineStyle(settings.lineBStyle);	  	                 	              	              	                
	             }
	         }
	     });		
	     panel.addSettingsItem("Band Line type:", c);
	  }   
	 
	 private void addMBLineStyleSettings(final BookmapSettingsPanel panel) {
	     String[] lineStyles = Stream.of(LineStyle.values()).map(Object::toString).toArray(String[]::new);
	     JComboBox<String> c = new JComboBox<>(lineStyles);
	     c.setSelectedItem(settings.lineMBStyle.toString());
	     c.setEditable(false);
	     c.addActionListener(new ActionListener() {
	         @Override
	         public void actionPerformed(ActionEvent e) {
	             int idx = c.getSelectedIndex();
	             if (idx != settings.lineMBStyle.ordinal()) {
	                 settings.lineMBStyle = LineStyle.values()[idx];
	             	
	                 MBandH.setLineStyle(settings.lineMBStyle);	                   
	                 MBandL.setLineStyle(settings.lineMBStyle);                        
	             }
	         }
	     });		
	     panel.addSettingsItem("mBand Line type:", c);
	}   

	 private void addBLineWidthSettings(final BookmapSettingsPanel panel) {
	      JComboBox<Integer> c = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
	      setAlignment(c);
	      c.setSelectedItem(settings.lineBWidth);
	      c.setEditable(false);
	      c.addActionListener(new ActionListener() {
	
	          @Override
	          public void actionPerformed(ActionEvent e) {
	              int newLineWidth = (int) c.getSelectedItem();
	              if (newLineWidth != settings.lineBWidth) {
	                  settings.lineBWidth = newLineWidth;
	                  BandH.setWidth(settings.lineBWidth);     //Why do i have this here and not elsewhere              
	                  BandL.setWidth(settings.lineBWidth); 		//Why do i have this here and not elsewhere   	                 	                                                
	              }
	          }
	      });
	      panel.addSettingsItem("Band Line width:", c);
	 }
	 
	 private void addMBLineWidthSettings(final BookmapSettingsPanel panel) {
	      JComboBox<Integer> c = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
	      setAlignment(c);
	      c.setSelectedItem(settings.lineMBWidth);
	      c.setEditable(false);
	      c.addActionListener(new ActionListener() {

	          @Override
	          public void actionPerformed(ActionEvent e) {
	              int newLineWidth = (int) c.getSelectedItem();
	              if (newLineWidth != settings.lineMBWidth) {
	                  settings.lineMBWidth = newLineWidth;
	                  
	                  MBandH.setWidth(settings.lineMBWidth);     
	                  MBandL.setWidth(settings.lineMBWidth);                   	                	                                                
	              }
	          }
	      });
	      panel.addSettingsItem("mBand Line width:", c);
	  }
 
	  private void setAlignment(final JComboBox<?> c) {
	      ((JLabel)c.getRenderer()).setHorizontalAlignment(JLabel.LEFT);
	  }
	  	 
    @Override
    public void stop() {
         api.setSettings(settings);
 		 Log.info("Bye");        
    }    
}
    