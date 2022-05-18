package IB;

//v1- Plots IndexBands with color set in UI. Uses HistoricalDataListener.
//v2- Same as v1 except that it............. DOES NOT Use HistoricalDataListener.
//v3- Had to delete v3. KEpt getting failed to enumerate entry points errors.
//v3- I took copy of v2 and v4 and named v3 and they still failed???
//v4- New color settings. Compiles and plots but new colors from CSV signal still Don't work.
//v5- copy of v4 for more testing. This had more complex color settings for later use after i figure out how to Double.NaN after change

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
//import velox.api.layer1.simplified.IndicatorModifiable;
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
@Layer1StrategyName("IB_v5")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class IB_v5 implements CustomModule, IntervalListener, /*HistoricalDataListener,*/ TimeListener, CustomSettingsPanelProvider {
 	
    @StrategySettingsVersion(currentVersion = 1, compatibleVersions = {})	   
    public static class Settings {
	     public Color colorBull = defBullc;
	     public Color colorBear = defBearc;
	     public Color colorMBull = defMBullc;
	     public Color colorMBear = defMBearc;	     
	  
	     public int lineBWidth = defBLineWidth;
	     public int lineMBWidth = defMBLineWidth;      
	     	     
	     public LineStyle lineBStyle = defBLineStyle;
	     public LineStyle lineMBStyle = defMBLineStyle;	     
		      
	     public boolean reloadOnChange = true;
    } 
    private static Color BandColor;
    private static Color MBandColor;
    
    
	protected Settings settings;
    protected Api api;
	
	protected Indicator bullBandH;
	protected Indicator bullBandL;
	protected Indicator MbullBandH;
	protected Indicator MbullBandL;	
	
	protected Indicator bearBandH;
	protected Indicator bearBandL;
	protected Indicator MbearBandH;
	protected Indicator MbearBandL;		
	
    
    private static final Color defBullc =  Color.GREEN;
    private static final Color defMBullc = Color.GREEN.darker();//.darker();
    private static final Color defBearc =  Color.RED;
    private static final Color defMBearc = Color.RED.darker();//.darker();    
    
    private static final int defBLineWidth = 4;     
    private static final int defMBLineWidth = 4;       
    
    private static final LineStyle defBLineStyle = LineStyle.SOLID;
    private static final LineStyle defMBLineStyle = LineStyle.DOT;    
    

    Double pipV  = 0.0;
    private long t;
 	protected String TimeStr; 
 	 
 	private Double Price = null;	
 	
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

		this.api = api;
        settings = api.getSettings(Settings.class);				
        this.symStr = info.symbol.toString();					//added 'this.'
		this.newStr = symStr.substring(0, symStr.length() - 2); //added 'this.' which seems to fix newStr from logging string value from second subscription 
		Log.info("Initialize " + symStr);
				
		this.finalfilepath = "C:\\Bookmap\\Notes\\BookMap_IB_ADDon_" + newStr + "(RITHMIC).csv"; 	// How do i chk if file exists?				 
        
		this.pipV = info.pips;
		
		this.bullBandH = api.registerIndicator("bullBandH",GraphType.PRIMARY);//added 'this.'
	    setBVisualProperties(bullBandH);											 //should i add 'this.' to these also?
	    bullBandH.setColor(defBullc);											 //should i add 'this.' to these also?
		
		this.bullBandL = api.registerIndicator("bullBandL",GraphType.PRIMARY);//added 'this.'
	    setBVisualProperties(bullBandL);											 //should i add 'this.' to these also?
	    bullBandL.setColor(defBullc);		
	    
		this.MbullBandH = api.registerIndicator("mbullBandH",GraphType.PRIMARY);//added 'this.'
	    setMBVisualProperties(MbullBandH);											 //should i add 'this.' to these also?
	    MbullBandH.setColor(defMBullc);											 //should i add 'this.' to these also?
		
		this.MbullBandL = api.registerIndicator("mbullBandL",GraphType.PRIMARY);//added 'this.'
	    setMBVisualProperties(MbullBandL);											 //should i add 'this.' to these also?
	    MbullBandL.setColor(defMBullc);	   
	    	    
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
				Log.info("ReadCSV- " + symStr +  " = " + p.getSymbol() + " PLevel " +  p.getPLevel() + " PName " + p.getPName() + " StartTime " + p.getStartTime() + " PTrend " + p.getPTrend());
				compiledata(p.getPName(),p.getPLevel(), p.getStartTime(),p.getPTrend() );
				
				//Log.info("ReadCSV- Compile Finished");
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
 		Log.info("getStrings " + plotStr);
    	return plotStr;
     }

	 public static LocalTime getTimes(String Stime) {// Should i add this anywhere inside this method?
		LocalTime TempTime = LocalTime.parse(Stime);		
		LocalTime TimeS = TempTime.minusMinutes(1);
		 			  						
		Log.info("getTiimes- " + " |TimeS " + TimeS.toString());				
		return TimeS;		
	 }		

	 public static Color getColor(String isBullish) {// Should i add this anywhere inside this method?
		 String isBull = isBullish;  
		 Color BandC = null;
		 Log.info("getColor TEST " );
		 
		if (isBull.equals("TRUE")) {
			BandC = Color.GREEN;		
			 Log.info("getColor TEST True " );
		}else if(isBull.equals("FALSE")) {
			BandC = Color.RED;
			 Log.info("getColor TEST False " );
		}else {
			Log.info("getColor TEST Else " );
			BandC = Color.GRAY;			
		}
	
		Log.info("getColor- " + " |BandC " + BandC.toString());				
		return BandC;		
	 }	
	 
	 public static Color getMColor(String isBullish) {// Should i add this anywhere inside this method?
		 String isBull = isBullish;  
		 Color MBandC = null; 
		 Log.info("getMColor TEST String= " + isBull.toString());

		if (isBull.equals("m_TRUE")) {
			Log.info("getMColor TEST True " );
			MBandC = Color.GREEN;									
		}else if(isBull.equals("m_FALSE")) {
			Log.info("getMColor TEST False " );
			MBandC = Color.RED;			
		}else {
			Log.info("getMColor TEST Else " );
			MBandC = Color.GRAY;			
		}
	
		Log.info("getMColor- " + " |MBandC " + MBandC.toString());				
		return MBandC;		
	 }		 
	 
	 
     public void compiledata(String PName, String Plevel, String Starttime, String PTrend) {		  	
    	String plotStr = getStrings(PName);
    	LocalTime TimeS = getTimes(Starttime);     	
		LocalDateTime TimeNow = localT(); 
		Color isBullish = getColor(PTrend);
		
		Log.info("compiledata TEST " );
		
		Color isMBullish = getMColor(PTrend);	
		
		//this.Price = Double.parseDouble(Plevel);			
		if (!Plevel.contains("999999")) {
			this.Price = Double.parseDouble(Plevel);		
		}else {
			this.Price = Double.NaN;		
		}
		
	 	Log.info("compileData- "  + symStr + " Time is "  + TimeNow.toString() + " |plotStr " + plotStr + " |Plevel " + Plevel.toString() + " |Starttime " + Starttime.toString() + " |PTrend " + PTrend.toString() + "|isBullish " + isBullish.toString() + "|isMBullish " + isMBullish.toString());		
		
		//onlyOnce();
	
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	 	
		//LocalTime time = LocalTime.parse(TimeStop);
		//String text = time.format(formatter);
		//LocalTime parsedTime = LocalTime.parse(text, formatter);
		
		// Boolean noPlotTimeRange = ( LocalTime.parse(TimeNow.format(formatter)).isAfter( TimeS )  &&  LocalTime.parse(TimeNow.format(formatter)).isBefore( TimeE ) ) ;
		// Boolean noPlotTimeRange = ( LocalTime.parse(TimeNow.format(formatter)).isBefore( TimeS )  &&  LocalTime.parse(TimeNow.format(formatter)).isAfter( TimeE ) ) ;
				  
		//  Log.info("compileDataBool- "  + plotStr + " | " + noPlotTimeRange.toString());
		   
		//Log.info("compileData- "  + symStr + " Time is "  + localT.toString() + " |TimeStop " + TimeStop + " |TimeStart " + TimeStart + " |TimeEnd " + TimeEnd + " |TimeEnd2 " + TimeEnd2 + " |ORLo " + ORLo + " |ORLo2 " + ORLo2 + " |ORLoBO " + ORLoBO + " |ORLoBO2 " + ORLoBO2 + " |ORHi " + ORHi + " |ORHi2 " + ORHi2 + " |ORHiBO " + ORHiBO + " |ORHiBO2 " + ORHiBO2);		
				
	      Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-   " + " |Price= " + Price + " |TimeS= " + TimeS );//+  " |Bool= " + noPlotTimeRange); 		
	    	    	
	    ///	if (plotStr.equals("bullBandH") && (noPlotTimeRange || Price.isNaN()) )  { 	    		   	    		   
	   	//	 	bullBandH.addPoint(Double.NaN);
		//	 	//Log.info("DONT PLOT bullBandH");  
	    //	}else 
	      		if (plotStr.equals("BHi") || plotStr.equals("BHi's")){
	    		//Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	      			bullBandH.setColor(isBullish);   	      			
	      			bullBandH.addPoint(Price/pipV);	      			   
		    }	
	    	   	
	    //	if (plotStr.equals("bullBandL") && (noPlotTimeRange || Price.isNaN()) ) { 	    		   	    		   
	    //		bullBandL.addPoint(Double.NaN);   	
			 	//Log.info("DONT PLOT bullBandL");  
	   // 	}else
	    		if (plotStr.equals("BLo") || plotStr.equals("BLo's")){
	    		//Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    			bullBandL.setColor(isBullish);   	    			
	    			bullBandL.addPoint(Price/pipV); 
		    }	
	    	
	    	//if (plotStr.equals("MbullBandH") && (noPlotTimeRange || Price.isNaN()) )  { 	    		   	    		   
	   		// 	MbullBandH.addPoint(Double.NaN);
			 	//Log.info("DONT PLOT MbullBandH");  
	    	//}else 
	    		if (plotStr.equals("mBHi")) {
	    		//Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    			MbullBandH.setColor(isMBullish); 
	    			MbullBandH.addPoint(Price/pipV);   			  
		    }	
	    		   	
	    	//if (plotStr.equals("MbullBandL") && (noPlotTimeRange || Price.isNaN()) ) { 	    		   	    		   
	    	//	MbullBandL.addPoint(Double.NaN);   	
			 	//Log.info("DONT PLOT MbullBandL");  
	    	//}else
	    		if (plotStr.equals("mBLo")) {
	    		//Log.info("The String is " + plotStr + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");	    				    		
	    			MbullBandL.setColor(isMBullish);	
	    			MbullBandL.addPoint(Price/pipV);      		
		    }	
	        	
	    Log.info("added POINTS!");   

	    //return true;
	    //prevT.compareTo(Tm) = TimeS;
	}
		 
	/* @Override
	 public void onBar(OrderBook orderBook, Bar bar) {
	  }   
	*/

	 protected void onSettingsChange() {
	 	Log.info("onSettingsChange " + localT()); 
        //api.reload();
	 	
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
		 BookmapSettingsPanel panel = new BookmapSettingsPanel("mBand settings");
		 addMBLineStyleSettings(panel);
		 addMBLineWidthSettings(panel);	 
		 addMBColorsSettings(panel);
		 return panel;
	 }	  
	 	 
	 private void addBColorsSettings(final BookmapSettingsPanel panel) {   
	        panel.addSettingsItem("Bullish color:", createColorsConfigItem(true));		
	        panel.addSettingsItem("Bearish color:", createColorsConfigItem(false));
	 }
	 
	 private void addMBColorsSettings(final BookmapSettingsPanel panel) {   
	       panel.addSettingsItem("Bullish color:", createMBColorsConfigItem(true));	
	       panel.addSettingsItem("Bearish color:", createMBColorsConfigItem(false));	        
	 }	
			 	 
	 private ColorsConfigItem createColorsConfigItem(boolean isBullish) {
	    Consumer<Color> c = new Consumer<Color>() {
	       @Override
	       public void accept(Color color) {    	    	   
	           if (isBullish) {
	               settings.colorBull = color;
	               bullBandH.setColor(settings.colorBull);
	               bullBandL.setColor(settings.colorBull);	               
	           } else{
	               settings.colorBear = color;
	               bearBandH.setColor(settings.colorBear);
	               bearBandL.setColor(settings.colorBear);	               
	           }   	
	       }	
	    };
	 
	    Color color = isBullish ? settings.colorBull : settings.colorBear;     
	    Color defaultColor = isBullish ? defBullc : defBearc;
		
	    //Log.info("TEST  " + x + " | " + color + " | " + ndefaultColor);
	    return new ColorsConfigItem(color, defaultColor, c);
	 }
	
	 
	 private ColorsConfigItem createMBColorsConfigItem(boolean isBullish) {
	    Consumer<Color> c = new Consumer<Color>() {
	       @Override
	       public void accept(Color color) {    	    	   
	           if (isBullish) {
	               settings.colorMBull = color;
	               MbullBandH.setColor(settings.colorMBull);
	               MbullBandL.setColor(settings.colorMBull);	               
	           } else{
	               settings.colorMBear = color;
	               MbearBandH.setColor(settings.colorMBear);
	               MbearBandL.setColor(settings.colorMBear);	               
	           }   	
	       }	
	   };
	 
	   Color color = isBullish ? settings.colorMBull : settings.colorMBear;     
	   Color defaultColor = isBullish ? defMBullc : defMBearc;
		
	   //Log.info("TEST  " + x + " | " + color + " | " + ndefaultColor);
	   return new ColorsConfigItem(color, defaultColor, c);
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
	                 bullBandH.setLineStyle(settings.lineBStyle);	                            
	                 bullBandL.setLineStyle(settings.lineBStyle);	  	                 	              	              	                
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
	             	
	                 MbullBandH.setLineStyle(settings.lineMBStyle);	                   
	                 MbullBandL.setLineStyle(settings.lineMBStyle);                        
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
	                  bullBandH.setWidth(settings.lineBWidth);     //Why do i have this here and not elsewhere              
	                  bullBandL.setWidth(settings.lineBWidth); 		//Why do i have this here and not elsewhere   	                 	                                                
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
	                  
	                  MbullBandH.setWidth(settings.lineMBWidth);     
	                  MbullBandL.setWidth(settings.lineMBWidth);                   	                	                                                
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
    }    
}
    