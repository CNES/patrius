package fr.cnes.sirius.patrius.forces.maneuvers.orbman;

import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;


/**
 * Generic interface which offers an unique service to compute a DV from a SpacecraftState object.
 * 
 * HISTORY
 * VERSION:4.4:DM:DM-2112:04/10/2019:[PATRIUS] Manoeuvres impulsionnelles sur increments orbitaux
 * END-HISTORY
 * 
 * @since 4.4
 */
public interface ImpulseParKepManeuver {
    
    /**
     * Method to compute the DV thanks to Keplerian parameters included in the Spacecraft state.
     * 
     * @param state S/C state
     * @throws PatriusException thrown if there is no solution
     */
    void computeDV(final SpacecraftState state) throws PatriusException;
}