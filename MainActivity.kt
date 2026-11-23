package com.example.efootballremote

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.math.hypot

class MainActivity : Activity() {
    private val port = 47321
    private var client: Socket? = null
    private var server: ServerSocket? = null
    private var status: TextView? = null
    private val bg = Color.rgb(16,22,35)
    private val fg = Color.WHITE
    private val buttons = listOf("PASS", "SHOOT", "THROUGH", "SPRINT", "SWITCH", "PRESS")
    private val prefs by lazy { getSharedPreferences("positions", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); home() }
    private fun label(s: String, size: Float = 17f) = TextView(this).apply { text=s; textSize=size; setTextColor(fg); gravity=Gravity.CENTER; setPadding(10,10,10,10) }
    private fun column() = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setBackgroundColor(bg); setPadding(22,12,22,12) }
    private fun row() = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
    private fun action(text: String, fn: ()->Unit) = Button(this).apply { this.text=text; setOnClickListener { fn() } }
    private fun home() { val root=column(); root.addView(label("eFootball Remote  •  Local Wi-Fi prototype",24f)); root.addView(label("Install this app on BOTH phones. Keep both on the same trusted Wi-Fi network."));
        val r=row(); r.addView(action("PHONE 1: CONTROLLER") { controllerSetup() }); r.addView(action("PHONE 2: RECEIVER") { receiver() }); root.addView(r); setContentView(root) }
    private fun controllerSetup() { val root=column(); root.addView(label("Enter Phone 2's Wi-Fi IP address")); val ip=EditText(this).apply { hint="192.168.1.20"; setSingleLine(true); setText(prefs.getString("host", "")); setTextColor(fg); setHintTextColor(Color.LTGRAY) }; root.addView(ip)
        status=label("Not connected"); root.addView(status); root.addView(action("CONNECT") { val host=ip.text.toString().trim(); prefs.edit().putString("host",host).apply(); thread { try { client?.close(); client=Socket(host,port); runOnUiThread { gamepad() } } catch(e: Exception) { runOnUiThread { status?.text="Connection failed: ${e.message}" } } } }); root.addView(action("BACK") { home() }); setContentView(root) }
    @Synchronized private fun send(msg: String) { thread { try { client?.getOutputStream()?.write((msg+"\n").toByteArray()) } catch(_:Exception) {} } }
    private fun gamepad() { val root=column(); root.addView(label("CONTROLLER • Hold the joystick to move • Tap buttons to play",16f)); val content=row()
        val stick=object : View(this) { val paint=android.graphics.Paint(3); var dx=0f; var dy=0f
            override fun onDraw(c:android.graphics.Canvas) { val cx=width/2f; val cy=height/2f; val radius=minOf(width,height)*.35f; paint.color=Color.DKGRAY; c.drawCircle(cx,cy,radius,paint); paint.color=Color.CYAN; c.drawCircle(cx+dx*radius,cy+dy*radius,radius*.32f,paint) }
            override fun onTouchEvent(e:MotionEvent):Boolean { val cx=width/2f; val cy=height/2f; val r=minOf(width,height)*.35f; if(e.actionMasked==MotionEvent.ACTION_UP||e.actionMasked==MotionEvent.ACTION_CANCEL){dx=0f;dy=0f;send("MOVE,0,0")} else { val x=(e.x-cx)/r; val y=(e.y-cy)/r; val m=maxOf(1f,hypot(x,y)); dx=x/m;dy=y/m;send("MOVE,$dx,$dy") }; invalidate();return true }
        }; content.addView(stick,LinearLayout.LayoutParams(0,280,1f)); val grid=GridLayout(this).apply { columnCount=3; rowCount=2 }; buttons.forEach { b -> grid.addView(action(b) { send("BTN,$b") },GridLayout.LayoutParams().apply { width=160; height=115 }) }; content.addView(grid); root.addView(content); root.addView(action("DISCONNECT") { client?.close(); client=null;home() }); setContentView(root) }
    private fun localIp():String { val interfaces=NetworkInterface.getNetworkInterfaces(); while(interfaces.hasMoreElements()){ val i=interfaces.nextElement(); val addresses=i.inetAddresses; while(addresses.hasMoreElements()){val a=addresses.nextElement(); if(a is Inet4Address && !a.isLoopbackAddress && a.isSiteLocalAddress) return a.hostAddress ?: "Unknown"} }; return "Unknown (check Wi-Fi settings)" }
    private fun receiver() { val root=column(); root.addView(label("RECEIVER • Phone 2",24f)); root.addView(label("Wi-Fi IP: ${localIp()}  •  TCP port $port")); root.addView(label("Enable Accessibility for eFootball Remote. Then return here and start listening.")); root.addView(action("OPEN ACCESSIBILITY SETTINGS") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) });
        root.addView(action("CALIBRATE BUTTON POSITIONS") { calibrate() }); status=label("Server stopped"); root.addView(status); root.addView(action("START RECEIVER") { startReceiver() }); root.addView(action("STOP RECEIVER") { stopReceiver() }); root.addView(action("BACK") { home() }); setContentView(root) }
    private fun startReceiver() { if(server!=null) return; thread { try { val s=ServerSocket(port); server=s; runOnUiThread { status?.text="Listening on port $port" }; while(!s.isClosed){ val peer=s.accept(); runOnUiThread { status?.text="Controller connected" }; thread { peer.use { socket -> socket.getInputStream().bufferedReader().forEachLine { line -> handle(line) } } } } } catch(e:Exception) { runOnUiThread { status?.text="Receiver stopped: ${e.message}" } } finally {server=null} } }
    private fun stopReceiver() { try {server?.close()}catch(_:Exception){};server=null;status?.text="Stopped" }
    private fun handle(line:String) { val service=TouchService.instance ?: return; val parts=line.split(","); val dm=resources.displayMetrics; val w=dm.widthPixels.toFloat(); val h=dm.heightPixels.toFloat()
        if(parts.size==2 && parts[0]=="BTN" && parts[1] in buttons){val name=parts[1]; service.touch(prefs.getFloat("${name}_x",.8f)*w,prefs.getFloat("${name}_y",.7f)*h)}
        if(parts.size==3 && parts[0]=="MOVE") {val dx=parts[1].toFloatOrNull()?.coerceIn(-1f,1f) ?: return;val dy=parts[2].toFloatOrNull()?.coerceIn(-1f,1f) ?: return; if(hypot(dx,dy)>.15f){val x=prefs.getFloat("JOY_x",.2f)*w;val y=prefs.getFloat("JOY_y",.7f)*h;service.swipe(x,y,(x+dx*w*.09f).coerceIn(0f,w),(y+dy*h*.16f).coerceIn(0f,h))} }
    }
    private fun calibrate() { val root=column();root.addView(label("Tap a control, then tap its approximate location on THIS screen. Values are saved as screen percentages."));val selector=Spinner(this);val options=listOf("JOY")+buttons;selector.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,options);root.addView(selector)
        val area=FrameLayout(this).apply { setBackgroundColor(Color.rgb(40,55,65));addView(label("TAP POSITION HERE\n(Use the same landscape orientation and button layout as eFootball)").apply { alpha=.65f },FrameLayout.LayoutParams(-1,-1)) }; area.setOnTouchListener { v,e -> if(e.action==MotionEvent.ACTION_DOWN){val key=options[selector.selectedItemPosition];prefs.edit().putFloat("${key}_x",e.x/v.width).putFloat("${key}_y",e.y/v.height).apply();Toast.makeText(this,"Saved $key",Toast.LENGTH_SHORT).show();true}else true };root.addView(area,LinearLayout.LayoutParams(-1,0,1f));root.addView(action("DONE") {receiver()});setContentView(root) }
    override fun onDestroy() {stopReceiver();client?.close();super.onDestroy()}
}
