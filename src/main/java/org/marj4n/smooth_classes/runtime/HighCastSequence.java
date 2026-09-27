package org.marj4n.smooth_classes.runtime;

/** Tick-exact choreography, independent of networking and world rendering. */
public final class HighCastSequence {
    public enum Phase { RISE, BEAM, DESCEND, DONE }
    public static final int BEAM_TICKS=200, PULSE_INTERVAL=4;
    public Phase phase=Phase.RISE;
    public double height;
    public int age,beamTicks;
    public void tick(){
        switch(phase){
            case RISE -> {
                height+=Math.min(age<12?.42:.18,Math.max(0,(3-height)*.35));
                if(3-height<=.025){height=3;phase=Phase.BEAM;beamTicks=0;}
            }
            case BEAM -> {if(++beamTicks>=BEAM_TICKS)phase=Phase.DESCEND;}
            case DESCEND -> {height=Math.max(0,height-.18);if(height<=.001){height=0;phase=Phase.DONE;}}
            case DONE -> {}
        }
        age++;
    }
    public boolean pulse(){return phase==Phase.BEAM&&beamTicks%PULSE_INTERVAL==0;}
}
