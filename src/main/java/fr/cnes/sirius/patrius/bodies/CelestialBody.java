/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3149:10/05/2022:[PATRIUS] Optimisation des reperes interplanetaires 
 * VERSION:4.9:DM:DM-3163:10/05/2022:[PATRIUS] Enrichissement des reperes planetaires 
 * VERSION:4.9:DM:DM-3164:10/05/2022:[PATRIUS] Amelioration de la gestion du GM (parametre gravitationnel)
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2703:18/05/2021: Acces facilite aux donnees d'environnement (application interplanetaire) 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
/*
 * 
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.forces.gravity.AbstractAttractionModel;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Interface for celestial bodies like Sun, Moon or solar system planets.
 * 
 * @author Luc Maisonobe
 * @see CelestialBodyFactory
 * @see IAUPole
 */
public interface CelestialBody extends PVCoordinatesProvider {

    /**
     * Get an ICRF, body centered frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to other
     * inertial frames.
     * </p>
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getRotatingFrameTrueModel()
     */
    CelestialBodyFrame getICRF() throws PatriusException;

    /**
     * Get an EME2000, body centered frame.
     * <p>
     * The frame is always bound to the body center, and its axes are colinear to Earth EME2000 frame.
     * </p>
     * 
     * @return an EME2000, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getEME2000() throws PatriusException;

    /**
     * Get an inertially oriented, body centered frame taking into account only constant part of IAU pole data with
     * respect to ICRF frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to other
     * inertial frames.
     * </p>
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getInertialFrameConstantModel() throws PatriusException;

    /**
     * Get an inertially oriented, body centered frame taking into account only constant and secular part of IAU pole
     * data with respect to ICRF frame.
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getInertialFrameMeanModel() throws PatriusException;

    /**
     * Get an inertially oriented, body centered frame taking into account constant, secular and harmonics part of IAU
     * pole data with respect to ICRF frame.
     * 
     * @return an inertially oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     */
    CelestialBodyFrame getInertialFrameTrueModel() throws PatriusException;

    /**
     * Get a body oriented, body centered frame taking into account only constant part of IAU
     * pole data with respect to inertially-oriented frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to the celestial
     * body.
     * </p>
     * 
     * @return a body oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getInertialFrameConstantModel()
     */
    CelestialBodyFrame getRotatingFrameConstantModel() throws PatriusException;

    /**
     * Get a body oriented, body centered frame taking into account constant and secular part of IAU
     * pole data with respect to mean equator frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to the celestial
     * body.
     * </p>
     * 
     * @return a body oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getInertialFrameMeanModel()
     */
    CelestialBodyFrame getRotatingFrameMeanModel() throws PatriusException;

    /**
     * Get a body oriented, body centered frame taking into account constant, secular and harmonics part of IAU
     * pole data with respect to true equator frame.
     * <p>
     * The frame is always bound to the body center, and its axes have a fixed orientation with respect to the celestial
     * body.
     * </p>
     * 
     * @return a body oriented, body centered frame
     * @exception PatriusException
     *            if frame cannot be retrieved
     * @see #getInertialFrameConstantModel()
     */
    CelestialBodyFrame getRotatingFrameTrueModel() throws PatriusException;
    
    /**
     * Get the geometric shape of the body.
     * 
     * @return geometric shape of the body
     */
    BodyShape getShape();
    
    /**
     * Set a geometric shape to the body.
     * 
     * @param shapeIn
     * 		  the shape of the body
     */
    void setShape(BodyShape shapeIn);
    
    /**
     * Get the name of the body.
     * 
     * @return name of the body
     */
    String getName();

    /**
     * Get the central attraction coefficient of the body.
     * <p>Warning: attraction model should not be null (it is not null by default)/</p>
     * 
     * @return central attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getGM();
    
	/**
	 * Set a central attraction coefficient to the body.
	 * 
	 * @param gmIn
     *        the central attraction coefficient
     *        (m<sup>3</sup>/s<sup>2</sup>)
	 */
    void setGM(double gmIn);
    
    /**
     * Get the IAU pole and primer meridians orientation.
     * 
     * @return the IAU Pole
     */
    IAUPole getIAUPole();
    
    /**
     * Set a IAU Pole to define the body frames.
     * 
     * @param iauPoleIn
     *        the IAU pole 
     */
    void setIAUPole(IAUPole iauPoleIn);
    
    /**
     * Get the attraction model of the body.
     * 
     * @return the attraction model
     */
    AbstractAttractionModel getAttractionModel();
    
    /**
     * Set an attraction model to the body.
     * 
     * @param attractionModelIn
     * 		  the attraction model 
     */
    void setAttractionModel(AbstractAttractionModel attractionModelIn);
    
    /**
     * Get the ephemeris of the body.
     * 
     * @return the ephemeris
     */
    CelestialBodyEphemeris getEphemeris();
    
    /**
     * Set an ephemeris to the body.
     * 
     * @param ephemerisIn
     *        the ephemeris 
     */
    void setEphemeris(CelestialBodyEphemeris ephemerisIn);
    
    /**
     * Returns a string representation of the body and its attributes.
     * @return a string representation of the body and its attributes
     */
    @Override
    String toString();

}
