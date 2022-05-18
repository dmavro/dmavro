package IB;

//v1- Plots IndexBands with color set in UI. Uses HistoricalDataListener.
//v2- Same as v1 except that it............. DOES NOT Use HistoricalDataListener.


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import velox.api.layer1.simplified.HistoricalDataListener;
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

import java.time.temporal.ChronoField;

@Layer1SimpleAttachable
@Layer1StrategyName("IB_v2")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IB_v2 implements CustomModule, IntervalListener/*, HistoricalDataListener*/, TimeListener, CustomSettingsPanelProvider {
 	
    @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})	   
    public static class Settings {
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
 	private Double prevPrice;	 	
 	private Boolean newPlotprice; 
 			
 	private volatile String symStr = ""; // should this be static and volatile?
 	private volatile String newStr = "";			    // should this be private only?
 	private volatile String finalfilepath;				// should this be private only?

		
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
        pipV = info.pips;

		Log.info("Initialize " + symStr);
		this.api = api;
        settings = api.getSettings(Settings.class);				
        this.symStr = info.symbol.toString();					//added 'this.'
		this.newStr = symStr.substring(0, symStr.length() - 2); //added 'this.' which seems to fix newStr from logging string value from second subscription 
		 		
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
			  //Log.info("readCSV " + symStr + " " + filePath);
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
					  	//Log.info("GotCSVDATA");
				  }
			 }
			 
			for(PlotVals p: plotV) {		
				Log.info("ReadCSV- " + symStr +  " = " + p.getSymbol() + " PLevel " +  p.getPLevel() + " Note " + p.getPName() + " StartTime " + p.getStartTime() + " PTrend " + p.getPTrend());
				compiledata(p.getPName(),p.getPLevel(), p.getStartTime(),p.getPTrend() );
				
				Log.info("ReadCSV- Compile Finished");
				//System.out.printf("[symbol=%s, pricelevel=%s, note=%s, fcolor=%s, bcolor=%s]\n", p.getSymbol(), p.getPLevel(), p.getNote(), p.getFColor(), p.getBColor());
			}		   
		  } 
		  catch (Exception ex) {
			  ex.printStackTrace();
		  } 
		  finally {
			  try {
				  reader.close();
				  Log.info("finally- Finished Loop " + symStr);
				  //addPoints(localT);	//Is this ok here?
			  } 
			  catch (Exception e) {
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
	     return Intervals.INTERVAL_15_SECONDS;// add parameter so u can set time updates
	 }

	 @Override
	 public void onInterval() {
		LocalDateTime tStamp = localT();
		Log.info("++++++++++++++++++ Begin Loop Thru at onInterval+++++++++++++++++");			
		Log.info("onINterval- "  + symStr + " Time is "  + tStamp.toString());	
		
	    readCsv(finalfilepath) ;			 	 
       //addPoints(localT); 			    	    
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
		
		Log.info("getTiimes- " + " |TimeS " + TimeS.toString());				
		return TimeS;		
	 }		
	 
     public void compiledata(String PName, String Plevel, String Starttime, String PTrend) {		  	
    	String plotStr = getStrings(PName);
    	LocalTime TimeS = getTimes(Starttime);     	
		LocalDateTime TimeNow = localT(); 
		
		//this.Price = Double.parseDouble(Plevel);		
		if (!Plevel.contains("999999")) {
			this.Price = Double.parseDouble(Plevel);		
		}else {
			this.Price = Double.NaN;		
		}
		
		//Add array to store prices so i can store prevprices
		
		//	int[] currentPrice 
	
		Log.info("compileData- "  + symStr + " Time is "  + TimeNow.toString() + " |plotStr " + plotStr + " |Price " + Price.toString() + " |TimeS " + TimeS.toString() );
		
		//onlyOnce();
	
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	 	
		//LocalTime time = LocalTime.parse(TimeStop);
		//String text = time.format(formatter);
		//LocalTime parsedTime = LocalTime.parse(text, formatter);
		
		// Boolean noPlotTimeRange = ( LocalTime.parse(TimeNow.format(formatter)).isAfter( TimeS )  &&  LocalTime.parse(TimeNow.format(formatter)).isBefore( TimeE ) ) ;
		// Boolean noPlotTimeRange = ( LocalTime.parse(TimeNow.format(formatter)).isBefore( TimeS )  &&  LocalTime.parse(TimeNow.format(formatter)).isAfter( TimeE ) ) ;
		
		
		//Log.info("compileData1- "  + plotStr + " |prevPrice " + prevPrice + " |Price " + Price);
		  
		// newPlotprice = ( prevPrice.equals(this.Price)) ;
		// Log.info("compileDataBool- "  + plotStr + " | " + newPlotprice.toString());
			  
		//  Log.info("compileDataBool- "  + plotStr + " | " + noPlotTimeRange.toString());
		   
		//Log.info("compileData- "  + symStr + " Time is "  + localT.toString() + " |TimeStop " + TimeStop + " |TimeStart " + TimeStart + " |TimeEnd " + TimeEnd + " |TimeEnd2 " + TimeEnd2 + " |ORLo " + ORLo + " |ORLo2 " + ORLo2 + " |ORLoBO " + ORLoBO + " |ORLoBO2 " + ORLoBO2 + " |ORHi " + ORHi + " |ORHi2 " + ORHi2 + " |ORHiBO " + ORHiBO + " |ORHiBO2 " + ORHiBO2);		
				
	      Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-   " + " |Price= " + Price + " |TimeS= " + TimeS );//+  " |Bool= " + noPlotTimeRange); 		
	    	    	
	    	//if (plotStr.equals("BHi") || plotStr.equals("BHi's") && (newPlotprice || Price.isNaN()) )  { 	    		   	    		   
	   		// 	BandH.addPoint(Double.NaN);
			// 	BandH.addPoint(this.Price/pipV);   
			// 	Log.info("DONT PLOT BandH");  
	    	//}else 
	    	if (plotStr.equals("BHi") || plotStr.equals("BHi's")){
	    		Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			 	BandH.addPoint(this.Price/pipV);   
		    }	
	    	
	   	
	    	//if (plotStr.equals("BLo") || plotStr.equals("BLo's") && (newPlotprice || Price.isNaN()) ) { 	    		   	    		   
	    	//	BandL.addPoint(Double.NaN);  
	    	//	BandL.addPoint(this.Price/pipV); 	    		
			// 	Log.info("DONT PLOT BandL");  
	    	//}else 
	    	if (plotStr.equals("BLo") || plotStr.equals("BLo's")){
	    		Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		BandL.addPoint(this.Price/pipV);   		    		
		    }	
	    	
	    	//if (plotStr.equals("mBHi") && (newPlotprice || Price.isNaN()) )  { 	    		    		
	   		// 	MBandH.addPoint(Double.NaN);
			// 	MBandH.addPoint(this.Price/pipV);  
			// 	Log.info("DONT PLOT MBandH");  
	    	//}else
	    	if (plotStr.equals("mBHi")) {
	    		Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			 	MBandH.addPoint(this.Price/pipV);   
		    }	
	    	
	   	
	    	//if (plotStr.equals("mBLo") && (newPlotprice || Price.isNaN()) ) { 	    		   	    		   
	    	//	MBandL.addPoint(Double.NaN);   	
	    	//	MBandL.addPoint(Price/pipV);   		    		
			// 	Log.info("DONT PLOT MBandL");  
	    	//}else
	    	if (plotStr.equals("mBLo")) {
	    		Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    		MBandL.addPoint(this.Price/pipV);   	
		    }	
	        	
	    Log.info("added POINTS!");   
		
	    //return true;
		
		prevPrice = Price;
	    //prevT.compareTo(Tm) = TimeS;
	}
		 
	/* @Override
	 public void onBar(OrderBook orderBook, Bar bar) {
	  }   
	*/

	 protected void onSettingsChange() {
	 	Log.info("onSettingsChange " + localT()); 
	 	
	 	//Log.infoFree(interval); 
	 	//System.out.println("onSettingsChange " + ts());
	 	// indicatorBid.addPoint(Double.NaN);
	
	 }
	    			
	 @Override
	 public StrategyPanel[] getCustomSettingsPanels() {
	     StrategyPanel p1 = getBStyleSettingsPanel();
	     StrategyPanel p2 = getMBStyleSettingsPanel();	
	     return new StrategyPanel[] { p1, p2};        
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
    