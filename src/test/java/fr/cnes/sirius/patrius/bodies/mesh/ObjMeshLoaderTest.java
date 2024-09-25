/**
 * HISTORY
 * VERSION:4.11:DM:DM-3288:22/05/2023:[PATRIUS] ID de facette pour un FacetBodyShape
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3167:10/05/2022:[PATRIUS] La methode ObJMeshLoader ne permet pas de lire des fichiers .obj 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */

package fr.cnes.sirius.patrius.bodies.mesh;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/*
 *
 * Unit tests for the class {@link ObjMeshLoader}
 */
public class ObjMeshLoaderTest {
    
    /** Path to .obj files. */
    private static final String PATH = System.getProperty("user.dir") + "/src/test/resources/obj/";
    
    /**
     * Tests the loading process of .obj files.
     * @throws PatriusException 
     */
    @Test 
    public void testParsingProcess() throws PatriusException{
        
        final HashMap<String, String> mapNameObjFile = new HashMap<>();
        // generic .obj file
        mapNameObjFile.put("cube", "cube.obj");
        // simple .obj with only vertex and face defined
        mapNameObjFile.put(null, "simple-cube.obj");
        
        for(final Map.Entry<String, String> nameObjFile : mapNameObjFile.entrySet()){
            final ObjMeshLoader genericFile = new ObjMeshLoader(PATH + nameObjFile.getValue());
            //check object name if provided
            Assert.assertEquals(nameObjFile.getKey(), genericFile.getName());
            // check size of vertices list
            Assert.assertEquals(8, genericFile.getVertices().size());
            // check size of faces list
            Assert.assertEquals(12, genericFile.getTriangles().length);
            // check first vertice
            Assert.assertEquals(new Vector3D(0, 0, 0), genericFile.getVertices().get(1).getPosition());
            // check last vertice 
            Assert.assertEquals(new Vector3D(1000, 1000, 1000), genericFile.getVertices().get(8).getPosition());
            
            // check first face 
            final Triangle first = genericFile.getTriangles()[0];
            Assert.assertEquals(new Vector3D(0, 0, 0), first.getVertices()[0].getPosition());
            Assert.assertEquals(new Vector3D(1000, 1000, 0), first.getVertices()[1].getPosition());
            Assert.assertEquals(new Vector3D(1000, 0, 0), first.getVertices()[2].getPosition());
            Assert.assertEquals(1, first.getID());
            
            // check last face 
            final Triangle last = genericFile.getTriangles()[genericFile.getTriangles().length - 1];
            Assert.assertEquals(new Vector3D(0, 0, 1000), last.getVertices()[0].getPosition());
            Assert.assertEquals(new Vector3D(1000, 1000, 1000), last.getVertices()[1].getPosition());
            Assert.assertEquals(new Vector3D(0, 1000, 1000), last.getVertices()[2].getPosition());
            Assert.assertEquals(genericFile.getTriangles().length, last.getID());
        }
    }
}
