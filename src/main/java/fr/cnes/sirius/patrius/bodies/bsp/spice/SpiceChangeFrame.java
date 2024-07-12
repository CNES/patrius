/**
 * Copyright 2023-2023 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class is made to support changes among a standard set of inertial coordinate
 * reference frames.
 * This class is based on the CHGIRF.for file from the SPICE library.
 * @author T0281925
 *
 */
public final class SpiceChangeFrame {

    /**
     * Length of an state array
     */
    public static final int STATE_LENGTH = 6;
    /**
     * List of inertial frames
     */
    private static final List<InertialFrames> FRAMES = new ArrayList<InertialFrames>();
    
    /**
     * Boolean indicating if it is the first time calling a method for initialization purposes.
     */
    private static boolean first = true;
    
    /**
     * J2000 string
     */
    private static final String J2000 = "J2000";
    
    /**
     * B1950 string
     */
    private static final String B1950 = "B1950";
    
    /**
     * FK4 string
     */
    private static final String FK4 = "FK4";
    
    /**
     * No rotation string
     */
    private static final String NO_ROTATION = "0.0  3";
    
    /**
     * Boolean indicating if _______ is ready.
     */
    private static boolean ready = false;
    
    /** 
     * List of transformations
     */
    private static final Array2DRowRealMatrix[] TRANS = new Array2DRowRealMatrix[SpiceFrame.NINERT];
    
    /**
     * Constructor
     */
    private SpiceChangeFrame(){
        // Nothing to do
    }
    
    /**
     * Initialize the list of inertial frames.
     */
    private static void init() {
        // The root frame is mostly for show. Rotate by 0 arc seconds
        // about the x-axis to obtain the identity matrix.
        FRAMES.add(new InertialFrames(J2000, J2000, "0.0  1"));
        
        // The B1950 reference frame is obtained by precessing the J2000
        // frame backwards from Julian year 2000 to Besselian year 1950,
        // using the 1976 IAU precession model.
        //
        // The rotation from B1950 to J2000 is
        //
        //    [ -z ]  [ theta ]  [ -zeta ]
        //          3          2          3
        //
        // So the rotation from J2000 to B1950 is the transpose,
        //
        //    [ zeta ]  [ -theta ]  [ z ]
        //            3           2      3
        //
        // The values for z, theta, and zeta are computed from the formulas
        // given in table 5 of [5].
        //
        //    z     =  1153.04066200330"
        //    theta =  1002.26108439117"
        //    zeta  =  1152.84248596724"
        FRAMES.add(new InertialFrames(B1950, J2000, "1152.84248596724 3  -1002.26108439117  2  1153.04066200330  3"));
        
        // The FK4 reference frame is derived from the B1950 frame by
        // applying the equinox offset determined by Fricke. This is just
        // the rotation
        //
        //    [ 0.525" ]
        //              3
        FRAMES.add(new InertialFrames(FK4, B1950, "0.525  3"));
        
        // The DE-118 reference frame is nearly identical to the FK4
        // reference frame. It is also derived from the B1950 frame.
        // Only the offset is different:
        //
        //    [ 0.53155" ]
        //                3
        // Standish uses two separate rotations,
        //
        //    [ 0.00073" ]  P [ 0.5316" ]
        //                3              3
        //
        // (where P is the precession matrix used above to define the
        // B1950 frame). The major effect of the second rotation is to
        // correct for truncating the magnitude of the first rotation.
        // At his suggestion, we will use the untruncated value, and
        // stick to a single rotation.
        FRAMES.add(new InertialFrames("DE-118", B1950, "0.53155  3"));
        
        // Most of the other DE reference frames may be defined relative
        // to either the DE-118 or B1950 frames. The values below are taken
        // from [4].
        //
        //    DE number   Offset from DE-118   Offset from B1950
        //    ---------   ------------------   -----------------
        //           96             +0.1209"            +0.4107"
        //          102             +0.3956"            +0.1359"
        //          108             +0.0541"            +0.4775"
        //          111             -0.0564"            +0.5880"
        //          114             -0.0213"            +0.5529"
        //          122             +0.0000"            +0.5316"
        //          125             -0.0438"            +0.5754"
        //          130             +0.0069"            +0.5247"
        //
        // We will use B1950 for now, since the offsets generally have
        // more significant digits.
        FRAMES.add(new InertialFrames("DE-96", B1950, "0.4107  3"));
        FRAMES.add(new InertialFrames("DE-102", B1950, "0.1359  3"));
        FRAMES.add(new InertialFrames("DE-108", B1950, "0.4775  3"));
        FRAMES.add(new InertialFrames("DE-111", B1950, "0.5880  3"));
        FRAMES.add(new InertialFrames("DE-114", B1950, "0.5529  3"));
        FRAMES.add(new InertialFrames("DE-122", B1950, "0.5316  3"));
        FRAMES.add(new InertialFrames("DE-125", B1950, "0.5754  3"));
        FRAMES.add(new InertialFrames("DE-130", B1950, "0.5247  3"));
        
        // The Galactic System II reference frame is defined by the
        // following rotations:
        //
        //         o          o            o
        //    [ 327  ]  [ 62.6  ]  [ 282.25  ]
        //            3          1            3
        //
        //  In the absence of better information, we will assume that
        //  it is derived from the FK4 frame. Converting the angles from
        //  degrees to arc seconds,
        //
        //       o
        //    327      = 1177200"
        //        o
        //    62.6     =  225360"
        //          o
        //    282.25   = 1016100"
        FRAMES.add(new InertialFrames("GALACTIC", FK4, "1177200.0  3  225360.0  1  1016100.0  3"));
        // According to Standish, the various DE-200 frames are identical
        // with J2000, because he rotates the ephemerides before releasing
        // them (in order to avoid problems like the one that this routine
        // is designed to solve). Because we have to have something, we
        // will use
        //
        //         o
        //    [ 0.0 ]
        //           3
        FRAMES.add(new InertialFrames("DE-200", J2000, NO_ROTATION));
        FRAMES.add(new InertialFrames("DE-202", J2000, NO_ROTATION));
        // The values for the transformation from J2000 to MARSIAU_MO
        // are derived from the constants given for the pole of Mars
        // on page 8-2 of reference [6].
        FRAMES.add(new InertialFrames("MARSIAU", J2000, "324000.0 3 133610.4 2 -152348.4 3"));
        // The value for the obliquity of the ecliptic at J2000  is
        // taken from page  114 of [7] equation 3.222-1.  This agrees
        // with the expression given in [5]
        FRAMES.add(new InertialFrames("ECLIPJ2000", J2000, "84381.448 1"));
        // The value for the obliquity of the ecliptic at B1950  is
        // taken from page  171 of [7].
        FRAMES.add(new InertialFrames("ECLIPB1950", B1950, "84404.836 1"));
        // The frame for DE-140 is simply DE-400 rotated by the rotation:
        //
        //  0.9999256765384668  0.0111817701197967  0.0048589521583895
        // -0.0111817701797229  0.9999374816848701 -0.0000271545195858
        // -0.0048589520204830 -0.0000271791849815  0.9999881948535965
        //
        // Note that the DE-400 frame is J2000.
        //
        // This matrix was transposed to give the transformation from DE-140 to J2000.
        // The Euler representation is is constructed. Then, angles were converted 
        // to the range from -180 to 180 degrees and converted to arcseconds.  
        // At this point we have the Euler representation from DE-140 to J2000.
        //    [ A1 ]  [ A2 ]  [ A3 ]
        //          3       2       3
        //
        // To get the Euler representation of the transformation from
        // J2000 to DE-140  we use.
        //
        //    [ -A3 ]  [ -A2 ] [ -A1 ]
        //           3        2       3
        //
        // This method was used because it yields a nicer form of
        // representation than the straight forward transformation.
        // Note that these numbers are quite close to the values used
        // for the transformation from J2000 to B1950
        FRAMES.add(new InertialFrames("DE-140", J2000, 
                                      "1152.71013777252 3  -1002.25042010533  2  1153.75719544491  3"));
        // The frame for DE-142 is simply DE-402 rotated by the rotation:
        //
        //  0.9999256765402605  0.0111817697320531  0.0048589526815484
        // -0.0111817697907755  0.9999374816892126 -0.0000271547693170
        // -0.0048589525464121 -0.0000271789392288  0.9999881948510477
        //
        // Note that the DE-402 frame is J2000.
        //
        // The Euler angles giving the transformation for J2000 to
        // DE-142 were constructed in the same way as the transformation
        // from J2000 to DE140.  Only the input matrix changed to use the
        // one given above.
        FRAMES.add(new InertialFrames("DE-142", J2000, 
                                      "1152.72061453864 3  -1002.25052830351  2  1153.74663857521  3"));
        // The frame for DE-143 is simply DE-403 rotated by the rotation:
        //
        //  0.9999256765435852  0.0111817743077255  0.0048589414674762
        // -0.0111817743300355  0.9999374816382505 -0.0000271622115251
        // -0.0048589414161348 -0.0000271713942366  0.9999881949053349
        //
        // Note that the DE-403 frame is J2000.
        //
        // The Euler angles giving the transformation for J2000 to
        // DE-143 were constructed in the same way as the transformation
        // from J2000 to DE140.  Only the input matrix changed to use the
        // one given above.
        FRAMES.add(new InertialFrames("DE-143", J2000, 
                                      "1153.03919093833 3  -1002.24822382286 2  1153.42900222357 3"));
        first = false;
    }
    
    /**
     * Return the index of one of the standard inertial reference
     * frames supported by IRFROT.
     * Based on the IRFNUM routine from the SPICE library
     * 
     * @param name Name of standard inertial reference frame.
     * @return integer containing the index of the frame
     */
    public static int intertialRefFrameNumber(final String name) {
        
        if (name.equalsIgnoreCase(J2000)) {
            return 1;
        }
        
        // We do the initialization only if the frame is not j2000
        if (first) {
            init();
        }
        
        // Check if the frame is any form of "default" including having spaces in the middle.
        if (name.trim().matches("(?i)d\\s*e\\s*f\\s*a\\s*u\\s*l\\s*t")) {
            // If the name is "DEFAULT" return the default frame.
            return 0;
        } else {
            return 1 + FRAMES.indexOf(new InertialFrames(name));
        }     
    }
    
    /**
     * Compute the matrix needed to rotate vectors between two standard inertial reference frames.
     * This method is based on the routine IRFROT of the SPICE library
     * 
     * @param refA Initial reference frame. 
     * @param refB Desired reference frame.
     * @return Rotation matrix to go from A to B.
     * @throws PatriusException if any of the input frames is not recognized.
     */
    public static Array2DRowRealMatrix frameRotationMatrix(final int refA, 
                                                           final int refB) throws PatriusException {
        
        // We do the initialization if it is needed
        if (first) {
            init();
        }
        // If it has not been done already, construct the transformation
        // from the root frame to each supported reference frame.
        //
        // Begin by constructing the identity matrix (rotating by zero
        // radians about the x-axis). Apply the rotations indicated in
        // the frame definition (from right to left) to get the incremental
        // rotation from the base frame. The final rotation is
        //
        //    R             = (R           ) (R          )
        //     root->frame      base->frame    root->base
        
        if (!ready) {
            for (int i = 0; i < SpiceFrame.NINERT; i++) {
                TRANS[i] = rotate(0,1);
                final String[] rots = FRAMES.get(i).getDefs().split("\\s+");
                
                for (int j = rots.length; j >= 2; j = j - 2) {
                    final int axis = Integer.parseInt(rots[j - 1]);
                    final double angle = Double.parseDouble(rots[j - 2]);
                    // Conversion of angles to radians
                    final double radAng = angle * ( MathLib.PI / 180.0 ) / 3600;
                    rotMat(radAng, axis, TRANS[i]);    
                }
                // We look for the index where the base of the current frame is defined in the frame list
                final int b = FRAMES.indexOf(new InertialFrames(FRAMES.get(i).getBase()));
                
                TRANS[i] = TRANS[i].multiply(TRANS[b]);
            }
            
            ready = true;
        }
        
        // If the transformations have been defined, we can proceed with
        // the business at hand: determining the rotation from one frame
        // to another. To get from frame A to frame B, the rotation is
        //
        //                                 T
        //    R     = (R       ) (R       )
        //     A->B     root->B    root->A
        //
        // If A and B are the same frame, the rotation is just the identity.
        // In theory, computing
        //
        //                                 T
        //    R     = (R       ) (R       )
        //     A->A     root->A    root->A
        //
        // should work, but why risk roundoff problems?
        if ( refA < 0 || refA >= SpiceFrame.NINERT) {
            throw new PatriusException(PatriusMessages.PDB_UNKNOWN_INERTIAL_ID, refA, refB, refA);
        } else if ( refB < 0 || refB >= SpiceFrame.NINERT) {
            throw new PatriusException(PatriusMessages.PDB_UNKNOWN_INERTIAL_ID, refA, refB, refB);
        } else if (refA == refB) {
            return rotate(0,1);
        } else {
            return TRANS[refB - 1].multiply(TRANS[refA - 1], true);
        }
        
    }
    
    /**
     * Calculate the 3x3 rotation matrix generated by a rotation
     * of a specified angle about a specified axis. This rotation
     * is thought of as rotating the coordinate system.
     * 
     * A rotation about the first, i.e. x-axis, is described by
     *
     *    .-                            -.
     *    |  1        0           0      |
     *    |  0   cos(theta)  sin(theta)  |
     *    |  0  -sin(theta)  cos(theta)  |
     *    `-                            -'
     *
     * A rotation about the second, i.e. y-axis, is described by
     *
     *    .-                            -.
     *    |  cos(theta)  0  -sin(theta)  |
     *    |      0       1        0      |
     *    |  sin(theta)  0   cos(theta)  |
     *    `-                            -'
     *
     * A rotation about the third, i.e. z-axis, is described by
     *
     *    .-                            -.
     *    |  cos(theta)  sin(theta)  0   |
     *    | -sin(theta)  cos(theta)  0   |
     *    |       0          0       1   |
     *    `-                            -'
     * 
     * This method is based on the ROTATE routine from the SPICE library
     * 
     * @param angle Angle of rotation (radians).
     * @param axis Axis of rotation (X=1, Y=2, Z=3).
     * @return A RealMatrix containing the rotation matrix.
     */
    private static Array2DRowRealMatrix rotate(final double angle, 
                                               final int axis) {
        // Define the axis
        final int[] indexs = {2, 0, 1, 2, 0};
        // Get the sine and cosine of ANGLE
        final double[] sc = MathLib.sinAndCos(angle);
        
        // Get indices for axes. The first index is for the axis of rotation.
        // The next two axes follow in right hand order (XYZ).  First get the
        // non-negative value of IAXIS mod 3 .
        final int temp = ( (axis % 3) + 3 ) % 3;
        final int i1 = indexs[temp];
        final int i2 = indexs[temp + 1];
        final int i3 = indexs[temp + 2];
        
        final double[][] mout = new double[3][3];
        mout[i1][i1] = 1;
        mout[i2][i1] = 0;
        mout[i3][i1] = 0;
        mout[i1][i2] = 0;
        mout[i2][i2] = sc[1];
        mout[i3][i2] = -sc[0];
        mout[i1][i3] = 0;
        mout[i2][i3] = sc[0];
        mout[i3][i3] = sc[1];
        
        return new Array2DRowRealMatrix(mout);   
    }
    
    /**
     * Apply a rotation of ANGLE radians about axis IAXIS to a matrix.
     * This rotation is thought of as rotating the coordinate system.
     * This method is based on the ROTMAT routine of the SPICE library
     * 
     * @param angle Angle of rotation (radians).
     * @param axis Axis of rotation (X=1, Y=2, Z=3).
     * @param mIn Matrix to be rotated on input, resulting rotated matrix on output
     */
    private static void rotMat(final double angle, 
                                               final int axis,
                                               final Array2DRowRealMatrix mIn ) {
        // Define the axis
        final int[] indexs = {2, 0, 1, 2, 0};
        // Get the sine and cosine of ANGLE
        final double[] sc = MathLib.sinAndCos(angle);
        
        // Get indices for axes. The first index is for the axis of rotation.
        // The next two axes follow in right hand order (XYZ).  First get the
        // non-negative value of IAXIS mod 3 .
        final int temp = ( (axis % 3) + 3 ) % 3;
        final int i1 = indexs[temp];
        final int i2 = indexs[temp + 1];
        final int i3 = indexs[temp + 2];
        
        final double[][] mout = new double[3][3];
        final double[][] m1 = mIn.getData();
        for (int i = 0; i <= 2; i++) {
            mout[i1][i] = m1[i1][i];
            mout[i2][i] = sc[1] * m1[i2][i] + sc[0] * m1[i3][i];
            mout[i3][i] = - sc[0] * m1[i2][i] + sc[1] * m1[i3][i];
        }
        
        mIn.setSubMatrix(mout, 0, 0);
    }
}
