package com.team1323.frc2018.subsystems;

import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.sensors.PigeonIMU;
import com.ctre.phoenix.sensors.PigeonIMU.PigeonState;
import com.ctre.phoenix.sensors.PigeonIMU_StatusFrame;
import com.team1323.frc2018.Ports;
import com.team254.lib.util.math.Rotation2d;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Pigeon {
	private static Pigeon instance = null;
	public static Pigeon getInstance(){
		if(instance == null){
			instance = new Pigeon();
		}
		return instance;
	}
	
	private PigeonIMU pigeon;
	PigeonIMU.FusionStatus fusionStatus = new PigeonIMU.FusionStatus();
    
	private Pigeon(){
		try{
			pigeon = new PigeonIMU(new TalonSRX(Ports.PIGEON_TALON));
			pigeon.setStatusFramePeriod(PigeonIMU_StatusFrame.BiasedStatus_2_Gyro, 5, 10);
			pigeon.setStatusFramePeriod(PigeonIMU_StatusFrame.BiasedStatus_6_Accel, 5, 10);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
	public boolean isGood(){
		return (pigeon.getState() == PigeonState.Ready) ? true : false;
	}
	
	public Rotation2d getAngle(){
		double [] ypr = new double[3];
		pigeon.getYawPitchRoll(ypr);
		return Rotation2d.fromDegrees(/*-pigeon.getFusedHeading(fusionStatus)*/-ypr[0]);
	}
	
	public double getRawAngle(){
		PigeonIMU.FusionStatus fusionStatus = new PigeonIMU.FusionStatus();
		return -pigeon.getFusedHeading(fusionStatus);
	}
	
	public void setAngle(double angle){
		pigeon.setFusedHeading(-angle*64, 10);
		pigeon.setYaw(-angle*64, 10);
	}
	
	public void outputToSmartDashboard(){
		SmartDashboard.putBoolean("Pigeon Good", isGood());
		SmartDashboard.putNumber("Pigeon Temp", pigeon.getTemp());
		SmartDashboard.putNumber("Pigeon Compass", pigeon.getAbsoluteCompassHeading());
	}
}
