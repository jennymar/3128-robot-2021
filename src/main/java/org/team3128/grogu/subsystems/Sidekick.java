package org.team3128.grogu.subsystems;

import org.team3128.common.utility.Log;
import org.team3128.common.utility.test_suite.CanDevices;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import org.team3128.grogu.subsystems.Constants;
import org.team3128.common.hardware.motor.LazyCANSparkMax;
import org.team3128.common.hardware.motor.LazyTalonFX;
import org.team3128.common.hardware.motor.LazyTalonSRX;
import org.team3128.common.hardware.motor.LazyVictorSPX;

import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.PIDSubsystem;
import edu.wpi.first.wpilibj.controller.PIDController;


public class Sidekick extends PIDSubsystem {
    public static enum ShooterState {
        OFF(0),
        LONG_RANGE(4800), // long range shooting
        MID_RANGE(4080), // mid range shooting
        SHORT_RANGE(2000); // short range shooting 3700

        public double shooterRPM;

        private ShooterState(double RPM) {
            this.shooterRPM = RPM;
        }
    }

    public static final Sidekick instance = new Sidekick();
    public static LazyTalonSRX SIDEKICK;

    public static boolean DEBUG = true;
    double current = 0;
    double error = 0;
    public double output = 0;
    double prevError = 0;

    int plateauCount = 0;

    // private StateTracker stateTracker = StateTracker.getInstance();
    public ShooterState SHOOTER_STATE = ShooterState.MID_RANGE;

    private Sidekick() {

        super(new PIDController(Constants.ShooterConstants.SIDEKICK_PID.kP, Constants.ShooterConstants.SIDEKICK_PID.kI, Constants.ShooterConstants.SIDEKICK_PID.kD));
        getController().setTolerance(Constants.ShooterConstants.RPM_THRESHOLD);
        //.setDistancePerPulse(ShooterConstants.kEncoderDistancePerPulse);


        configMotors();
        configEncoders();
        setSetpoint(0);
    }

    public boolean atSetpoint() {
        return m_controller.atSetpoint();
    }

    public boolean isPlateaued() {
        return (plateauCount >= Constants.ShooterConstants.PLATEAU_COUNT);
    }

    private void configMotors() {
        SIDEKICK = new LazyTalonSRX(Constants.ShooterConstants.SHOOTER_SIDEKICK_ID);
        SIDEKICK.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, 0,
                Constants.ShooterConstants.CAN_TIMEOUT);
        SIDEKICK.setInverted(true);
        SIDEKICK.setSensorPhase(true);
        if (DEBUG) {
            Log.info("Shooter", "Config motors");
        }
    }

    private void configEncoders() {
        // SHOOTER_ENCODER = LEFT_SHOOTER.getEncoder();
        // if (DEBUG) {
        //     Log.info("Shooter", "Config encoders");
        // }
    }

    public static Sidekick getInstance() {
        return instance;
    }

    // @Override
    // public double getMeasurement() {
    //     //Log.info("shooter", "getting measurement");
    //     return LEFT_SHOOTER.getSelectedSensorVelocity(0) * 10 * 60 / Constants.MechanismConstants.ENCODER_RESOLUTION_PER_ROTATION;
    // }

    @Override
    public double getMeasurement() {
        return SIDEKICK.getSelectedSensorVelocity() * 10 * 60 / 4096;
    }

    @Override
    public void useOutput(double output, double setpoint) {
        double voltageOutput = shooterFeedForward(setpoint) + output;
        double voltage = RobotController.getBatteryVoltage(); // TODO: investigate bus voltage

        output = voltageOutput / voltage;

        //Log.info("Shooter", "using output");

        prevError = error;

        if ((Math.abs(error) <= Constants.ShooterConstants.RPM_THRESHOLD) && (setpoint != 0)) {
            plateauCount++;
        } else {
            plateauCount = 0;
        }

        if (output > 1) {
            // Log.info("SHOOTER",
            // "WARNING: Tried to set power above available voltage! Saturation limit SHOULD
            // take care of this ");
            output = 1;
        } else if (output < -1) {
            // Log.info("SHOOTER",
            // "WARNING: Tried to set power above available voltage! Saturation limit SHOULD
            // take care of this ");
            output = -1;
        }

        if(setpoint == 0) {
            output = 0;
        }

        SIDEKICK.set(ControlMode.PercentOutput, output);
    }

    public void setSetpoint(double passedSetpoint) {
        plateauCount = 0;
        super.setSetpoint(passedSetpoint);
        //Log.info("Shooter", "Set setpoint to" + String.valueOf(setpoint));
    }

    public void setState(ShooterState shooterState) {
        SHOOTER_STATE = shooterState;
        setSetpoint(shooterState.shooterRPM);
    }

    //@Override
    //public void periodic() {
        //Log.info("Shooter", "running periodic");
       
    //}

    public double shooterFeedForward(double desiredSetpoint) {
        //double ff = (0.00211 * desiredSetpoint) - 2; // 0.051
        double ff = (0.00245 * desiredSetpoint); //0.00147x - 0.2; // 0
        if (getSetpoint() != 0) {
            return ff;
        } else {
            return 0;
        }
    }

    // public double getRPMFromDistance() {
    //     return stateTracker.getState().targetShooterState.shooterRPM;
    // }

    // public boolean isReady() {
    //     return (plateauCount > Constants.PLATEAU_COUNT);
    // }

    // public void queue(){
    //     setState(stateTracker.getState().targetShooterState);
    // }

    public boolean isReady() {
        //return (isPlateaued());
        return true;
    }

    public void setPower(double power) {
        SIDEKICK.set(ControlMode.PercentOutput, power);
    }
}
