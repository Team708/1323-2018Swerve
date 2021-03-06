package com.team1323.frc2018.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.StatusFrame;
import com.ctre.phoenix.motorcontrol.VelocityMeasPeriod;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import com.team1323.frc2018.Constants;
import com.team1323.frc2018.loops.Looper;
import com.team1323.lib.util.Util;
import com.team254.lib.util.math.RigidTransform2d;
import com.team254.lib.util.math.Rotation2d;
import com.team254.lib.util.math.Translation2d;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import com.revrobotics.CANSparkMax;             
import com.revrobotics.CANEncoder;          
import com.revrobotics.CANPIDController;
import com.revrobotics.CANSparkMax.IdleMode;       
import com.revrobotics.CANSparkMaxLowLevel.MotorType;


public class SwerveDriveModule extends Subsystem{
	TalonSRX rotationMotor; 
	CANSparkMax driveMotor;  
	CANPIDController  drivePIDController;
	CANEncoder driveEncoder;

	int moduleID;
	String name = "Module ";
	double rotationSetpoint = 0;
	double driveSetpoint = 0;
	double currentAngle = 0;
	int encoderOffset;
	int encoderReverseFactor = 1;

	private double           previousEncDistance = 0;
	private Rotation2d       previousWheelAngle  = new Rotation2d();
	private Translation2d    position;
	private Translation2d    startingPosition;
	private RigidTransform2d estimatedRobotPose  = new RigidTransform2d();
	
	public SwerveDriveModule(int rotationSlot, int driveSlot, int moduleID, 
			int encoderOffset, Translation2d startingPose){
		rotationMotor = new TalonSRX(rotationSlot);
		driveMotor    = new CANSparkMax(driveSlot, MotorType.kBrushless);  
		drivePIDController  = new CANPIDController(driveMotor); 
		driveEncoder = new CANEncoder(driveMotor);   
		
		configureMotors();
		this.moduleID = moduleID;
		name += (moduleID + " ");
		this.encoderOffset = encoderOffset;
		previousEncDistance = 0;
		position = startingPose;
		this.startingPosition = startingPose;
		updateRawAngle();
	}
	
	public synchronized void invertDriveMotor(boolean invert){
		driveMotor.setInverted(invert);
	}
	
	public synchronized void invertRotationMotor(boolean invert){
		rotationMotor.setInverted(invert);
	}
	
	public synchronized void reverseDriveSensor(boolean reverse){
		driveMotor.setInverted(reverse);
	}
	
	public synchronized void reverseRotationSensor(boolean reverse){
		encoderReverseFactor = reverse ? -1 : 1;
		rotationMotor.setSensorPhase(reverse);
	}
	
	private void configureMotors(){
    	rotationMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Absolute, 0, 10);
    	//resetRotationToAbsolute();
    	rotationMotor.setSensorPhase(true);
    	rotationMotor.setInverted(false);
    	rotationMotor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 10, 10);
    	rotationMotor.enableVoltageCompensation(true);
    	rotationMotor.setNeutralMode(NeutralMode.Brake);
    	rotationMotor.configVoltageCompSaturation(7.0, 10);
    	rotationMotor.configNominalOutputForward(0.0, 10);
    	rotationMotor.configNominalOutputReverse(0.0, 10);
    	rotationMotor.configAllowableClosedloopError(0, 0, 10);
    	rotationMotor.configMotionAcceleration((int)(Constants.SWERVE_ROTATION_MAX_SPEED*10), 10);
    	rotationMotor.configMotionCruiseVelocity((int)(Constants.SWERVE_ROTATION_MAX_SPEED*0.8), 10);//0.8
    	rotationMotor.selectProfileSlot(0, 0);
    	rotationMotor.config_kP(0, 4.0, 10);//4
    	rotationMotor.config_kI(0, 0.0, 10);
    	rotationMotor.config_kD(0, 120.0, 10);//80
    	rotationMotor.config_kF(0, 1023.0/Constants.SWERVE_ROTATION_MAX_SPEED, 10);
		rotationMotor.set(ControlMode.MotionMagic, rotationMotor.getSelectedSensorPosition(0));
		
		// section removed and added JNP
		// driveMotor.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0, 10);
		drivePIDController.setFeedbackDevice(driveEncoder);

		// driveMotor.setSelectedSensorPosition(0, 0, 10);
		driveEncoder.setPosition(0.0);

		// driveMotor.setStatusFramePeriod(StatusFrame.Status_2_Feedback0, 10, 10);
    	// driveMotor.configVelocityMeasurementPeriod(VelocityMeasPeriod.Period_10Ms, 10);
    	// driveMotor.configVelocityMeasurementWindow(32, 10);
    	// driveMotor.configNominalOutputForward(2.0/12.0, 10);
    	// driveMotor.configNominalOutputReverse(-2.0/12.0, 10);
    	// driveMotor.configPeakOutputForward(1.0, 10);
    	// driveMotor.configPeakOutputReverse(-1.0, 10);
    	// driveMotor.configVoltageCompSaturation(12.0, 10);
		// driveMotor.enableVoltageCompensation(true);
    	// driveMotor.configOpenloopRamp(0.25, 10);
		// driveMotor.configAllowableClosedloopError(0, 0, 10);
		
		// driveMotor.setInverted(false);
		// driveEncoder.setInverted(false);

		// driveMotor.setSensorPhase(false);
		
		// driveMotor.setNeutralMode(NeutralMode.Brake);
		// driveMotor.setIdleMode(IdleMode.kBrake);
		driveMotor.setIdleMode(IdleMode.kCoast);

		// driveMotor.selectProfileSlot(0, 0);
		
		// driveMotor.config_kP(0, 0.2, 10);
		drivePIDController.setP(0.2);

		// driveMotor.config_kI(0, 0.0, 10);
		drivePIDController.setI(0);

		// driveMotor.config_kD(0, 24.0, 10);
		drivePIDController.setD(24);

    	// driveMotor.config_kF(0, 1023.0/Constants.SWERVE_DRIVE_MAX_SPEED, 10);
    	// driveMotor.configMotionCruiseVelocity((int)(Constants.SWERVE_DRIVE_MAX_SPEED*0.9), 10);
    	// driveMotor.configMotionAcceleration((int)(Constants.SWERVE_DRIVE_MAX_SPEED), 10);
	}
	
	private double updateRawAngle(){
		currentAngle = encUnitsToDegrees(rotationMotor.getSelectedSensorPosition(0));
		return currentAngle;
	}
	
	public Rotation2d getModuleAngle(){
		return Rotation2d.fromDegrees(currentAngle - encUnitsToDegrees(encoderOffset));
	}
	
	public Rotation2d getFieldCentricAngle(Rotation2d robotHeading){
		Rotation2d normalizedAngle = getModuleAngle();
		return normalizedAngle.rotateBy(robotHeading);
	}
	
	public void setModuleAngle(double goalAngle){
		double newAngle = Util.placeInAppropriate0To360Scope(currentAngle, goalAngle + encUnitsToDegrees(encoderOffset));
		rotationMotor.set(ControlMode.MotionMagic, degreesToEncUnits(newAngle));
		rotationSetpoint = degreesToEncUnits(newAngle);
	}
	
	public void setRotationOpenLoop(double power){
		rotationMotor.set(ControlMode.PercentOutput, power);
		rotationSetpoint = power;
	}
	
	public void setDriveOpenLoop(double power){
		
		driveMotor.set(power);
		// driveMotor.set(ControlMode.PercentOutput, power);
		
		driveSetpoint = power;
	}
	
	public void setDrivePositionTarget(double deltaDistanceInches){
		// double setpoint = driveMotor.getSelectedSensorPosition(0) + inchesToEncUnits(deltaDistanceInches);  // JNP removed
		double setpoint = driveEncoder.getPosition() + inchesToEncUnits(deltaDistanceInches);  //JNP added
	
		// driveMotor.set(ControlMode.MotionMagic, setpoint);  //JNP removed
		driveEncoder.setPosition(setpoint);  //JNP added
		driveSetpoint = driveSetpoint;
	}
	
	public boolean drivePositionOnTarget(){
		// if(driveMotor.getControlMode() == ControlMode.MotionMagic)  //FIX JNP
		// 	return encUnitsToInches((int)Math.abs(driveSetpoint - driveMotor.(0))) < 2.0;

		return false;
	}
	
	private double getDriveDistanceFeet(){
		return getDriveDistanceInches() / 12.0;
	}
	
	private double getDriveDistanceInches(){
		// return encUnitsToInches(driveMotor.getSelectedSensorPosition(0));  //JNP removed
		return encUnitsToInches(driveEncoder.getPosition());
	}
	
	public double encUnitsToInches(double encUnits){
		return encUnits/Constants.SWERVE_ENC_UNITS_PER_INCH;
	}
	
	public int inchesToEncUnits(double inches){
		return (int) (inches*Constants.SWERVE_ENC_UNITS_PER_INCH);
	}
	
	public double encUnitsPer100msToFeetPerSecond(int encUnitsPer100ms){
		return encUnitsToInches(encUnitsPer100ms) / 12.0 * 10;
	}
	
	public int degreesToEncUnits(double degrees){
		return (int) (degrees/360.0*Constants.ROTATION_ENCODER_RESOLUTION);
	}
	
	public double encUnitsToDegrees(int encUnits){
		return encUnits/Constants.ROTATION_ENCODER_RESOLUTION*360.0;
	}
	
	public Translation2d getPosition(){
		return position;
	}
	
	public RigidTransform2d getEstimatedRobotPose(){
		return estimatedRobotPose;
	}
	
	public synchronized void updatePose(Rotation2d robotHeading){
		double currentEncDistance = getDriveDistanceFeet();
		double deltaEncDistance = (currentEncDistance - previousEncDistance) * Constants.kWheelScrubFactors[moduleID];
		updateRawAngle();
		Rotation2d currentWheelAngle = getFieldCentricAngle(Pigeon.getInstance().getAngle());
		currentWheelAngle.normalize();
		Rotation2d averagedAngle = Rotation2d.fromDegrees((currentWheelAngle.getDegrees() + previousWheelAngle.getDegrees())/2.0);
		Translation2d deltaPosition = new Translation2d(currentWheelAngle.cos()*deltaEncDistance, 
				currentWheelAngle.sin()*deltaEncDistance);
		Translation2d updatedPosition = position.translateBy(deltaPosition);
		RigidTransform2d staticWheelPose = new RigidTransform2d(updatedPosition, robotHeading);
		RigidTransform2d robotPose = staticWheelPose.transformBy(RigidTransform2d.fromTranslation(startingPosition).inverse());
		position = updatedPosition;
		estimatedRobotPose =  robotPose;
		previousEncDistance = currentEncDistance;
		previousWheelAngle = currentWheelAngle;
	}
	
	public synchronized void resetPose(RigidTransform2d robotPose){
		Translation2d modulePosition = robotPose.transformBy(RigidTransform2d.fromTranslation(startingPosition)).getTranslation();
		position = modulePosition;
	}
	
	public synchronized void resetPose(){
		position = startingPosition;
	}
	
	@Override
	public synchronized void stop(){
		//setModuleAngle(getModuleAngle().getDegrees());
		//setRotationOpenLoop(0.0);
		setDriveOpenLoop(0.0);
	}
	
	public synchronized void resetRotationToAbsolute(){
		rotationMotor.setSelectedSensorPosition(
				encoderReverseFactor * (rotationMotor.getSensorCollection().getPulseWidthPosition() - encoderOffset), 0, 10);
	}

	@Override
	public synchronized void zeroSensors() {
		zeroSensors(new RigidTransform2d());
	}
	
	public synchronized void zeroSensors(RigidTransform2d robotPose) {
		// driveMotor.setSelectedSensorPosition(0, 0, 0);
		// driveEncoder.set(0);  // jnp removed
		driveEncoder.setPosition(0.0);
		//resetRotationToAbsolute();
		resetPose(robotPose);
		estimatedRobotPose = robotPose;
		previousEncDistance = 0;
		previousWheelAngle = getFieldCentricAngle(robotPose.getRotation());
	}

	@Override
	public void registerEnabledLoops(Looper enabledLooper) {
		
	}

	@Override
	public void outputToSmartDashboard() {
		updateRawAngle();
		SmartDashboard.putNumber(name + "Angle", getModuleAngle().getDegrees());
		SmartDashboard.putNumber(name + "Pulse Width", rotationMotor.getSelectedSensorPosition(0));
		SmartDashboard.putNumber(name + "Drive Voltage", driveMotor.getVoltageCompensationNominalVoltage()); //JNP added
		SmartDashboard.putNumber(name + "Inches Driven", getDriveDistanceInches());
		// if(rotationMotor.getControlMode() == ControlMode.MotionMagic)
		// 	SmartDashboard.putNumber(name + "Error", rotationMotor.getClosedLoopError(0));
		SmartDashboard.putNumber(name + "X", position.x());
		SmartDashboard.putNumber(name + "Y", position.y());
		// SmartDashboard.putNumber(name + "Drive Current", driveMotor.getOutputCurrent());
	}
	
}
