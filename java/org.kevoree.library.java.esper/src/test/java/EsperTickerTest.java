import org.kevoree.annotation.*;

@ComponentType
public class EsperTickerTest {


	boolean s;
    @Output
    org.kevoree.api.Port out;

    
    Thread t =null;
    @Start
    public void start() {
    	this.s = true;
    	t= new Thread(new Runnable() {
			public void run() {
				while(s){
					out.send( ""+Math.random()*100, null);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		});
    	t.start();
    	
    }

    @Stop
    public void stop() {
    	this.s = false;

    }

    @Update
    public void update() {
    	this.stop();
    	this.start();
    	
    	
    }

}

