<%@ page import="com.draagon.timers.*,java.util.*" %>

<html>
<head>
    <title>Method Timers</title>        
</head>
<body>
        
<h1>Method Timers</h1>
<h4>These timers represent the execution time of methods within the system.  High execution times are colored from yellow to orange to red for severe time delays.</h4>

<br> 

<input type="button" value="Refresh" onClick="javascript:history.go(0)"/>

<br>

<table border=0 width=100%>
<tr><th rowspan=2 valign=top>Name</th><th colspan=4>Overall</th><th colspan=4>Since Last</th></tr>
<tr><th><font size=-1>Cnt</th><th><font size=-1>Avg</th><th><font size=-1>Min</th><th><font size=-1>Max</th><th><font size=-1>Cnt</th><th><font size=-1>Avg</th><th><font size=-1>Min</th><th><font size=-1>Max</th></tr>
<%
  for( Iterator packages = MethodTimer.getAllPackages().iterator(); packages.hasNext(); )
  {
    String p = (String) packages.next();

    out.println( "<tr><th colspan=9><font size=-1>" + p + "</font></th></tr>" );

    for( Iterator classes = MethodTimer.getAllClasses( p ).iterator(); classes.hasNext(); )
    {
     String c = (String) classes.next();

     String name = c;
     int ii = name.lastIndexOf( '.' );
     if ( ii >= 0 ) name = name.substring( ii + 1 );

     out.println( "<tr bgcolor=#dddddd><td colspan=9 align=left><font size=-1><b>" + name + "</b></font></td></tr>" );

     for( Iterator timers = MethodTimer.getAllTimers( p, c ).iterator(); timers.hasNext(); )
     {
       MethodTimer mt = (MethodTimer) timers.next();
       long t [] = mt.getValues();

       String bgcolor = "#ffffff";
       if ( t[ 1 ] >= 300 && t[ 1 ] < 500 ) bgcolor = "#ffffcc";
       else if ( t[ 1 ] >= 500 && t[ 1 ] < 1000 ) bgcolor = "#ffff00";
       else if ( t[ 1 ] >= 1000 && t[ 1 ] < 2000 ) bgcolor = "#ffaa00";
       else if ( t[ 1 ] >= 2000 ) bgcolor = "#ff5555";

       if ( bgcolor != null )
         out.println( "<tr bgcolor=" + bgcolor +">" );
       else
         out.println( "<tr>" );

       out.println( "<td><font size=-1>&nbsp;&nbsp;" + mt.getName() + "</font>"
           + "</td><td align=center><font size=-1>" + t[ MethodTimer.TOTAL_ACCESS_COUNT ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.TOTAL_AVERAGE ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.TOTAL_MIN_TIME ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.TOTAL_MAX_TIME ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.CURRENT_ACCESS_COUNT ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.CURRENT_AVERAGE ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.CURRENT_MIN_TIME ] + "</td>"
           + "<td align=center><font size=-1>" + t[ MethodTimer.CURRENT_MAX_TIME ] + "</td></tr>" );
     }
    }
  }
 %>

</table>

<br>
<input type="button" value="Refresh" onClick="javascript:history.go(0)"/>

</body>
</html>
