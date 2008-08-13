/**
 * Copyright 2008 code_swarm project team
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import javax.vecmath.Vector2f;

/**
 * @brief Physics Engine implementation.  In essence, people bounce around.  Nodes are attracted to the people.
 * 
 * @see PhysicsEngine for interface information
 * @author Desmond Daignault  <nawglan at gmail>
 */
public class PhysicsEngineChaotic implements PhysicsEngine
{
  private CodeSwarmConfig cfg;
  
  private float DRAG;
  
  
  /**
   * Method for initializing parameters.
   * @param p Properties from the config file.
   */
  public void setup (CodeSwarmConfig p)
  {
    cfg = p;
    DRAG = cfg.getFloatProperty("drag",0.00001);
  }
  
  /**
   * Method to ensure upper and lower bounds
   * @param value Value to check
   * @param min Floor value
   * @param max Ceiling value
   * @return value if between min and max, min if < max if >
   */
  private float constrain(float value, float min, float max) {
    if (value < min) {
      return min;
    } else if (value > max) {
      return max;
    }
    
    return value;
  }
  
  /**
   * Legacy method that calculate the attractive/repulsive force between a person and one of its file along their link (the edge).
   * 
   * @param edge the link between a person and one of its file 
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceAlongAnEdge( code_swarm.Edge edge )
  {
    float distance;
    float deltaDistance;
    Vector2f force = new Vector2f();
    Vector2f tforce = new Vector2f();
    
    // distance calculation
    tforce.sub(edge.nodeTo.mPosition, edge.nodeFrom.mPosition);
    distance = tforce.length();
    if (distance > 0) {
      // force calculation (increase when distance is different from targeted len")
      deltaDistance = (edge.len - distance) / (distance * 3);
      // force ponderation using a re-mapping life from 0-255 scale to 0-1.0 range
      // This allows nodes to drift apart as their life decreases.
      deltaDistance *= ((float)edge.life / edge.LIFE_INIT);
      
      // force projection onto x and y axis
      tforce.scale(deltaDistance);
      
      force.set(tforce);
    }
    
    return force;
  }
  
  /**
   * Legacy method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceBetweenfNodes( code_swarm.FileNode nodeA, code_swarm.FileNode nodeB )
  {
    float distance;
    Vector2f force = new Vector2f();
    Vector2f normVec = new Vector2f();
    
    /**
     * Get the distance between nodeA and nodeB
     */
    normVec.sub(nodeA.mPosition, nodeB.mPosition);
    distance = normVec.lengthSquared();
    /**
     * If there is a Collision.  This is assuming a radius of zero.
     * if (lensq == (radius1 + radius2)) is what to use if we have radius 
     * could use touches for files and edge_length for people?
     */
    if (distance == (nodeA.touches + nodeB.touches)) {
      force.set(0.01f* (((float)Math.random()*2)-1), (0.01f* ((float)Math.random()*2)-1));
    } else if (distance < 10000) {
      /**
       * No collision and distance is close enough to actually matter.
       */
      normVec.scale(1/distance);
      force.set(normVec);
    }
    
    return force;
  }
  
  /**
   * Legacy method that calculate the repulsive force between two similar nodes (either files or persons).
   * 
   * @param nodeA [in]
   * @param nodeB [in]
   * @return force force calculated between those two nodes
   */
  private Vector2f calculateForceBetweenpNodes( code_swarm.PersonNode nodeA, code_swarm.PersonNode nodeB )
  {
    Vector2f force = new Vector2f();
    Vector2f tmp = new Vector2f();
    
    if ((nodeA.life <= 0) || (nodeB.life <= 0)) {
      return  force;
    }
    
    tmp.sub(nodeA.mPosition, nodeB.mPosition);
    double distance = Math.sqrt(tmp.lengthSquared());
    if (distance <= (nodeA.mass + nodeB.mass)) {
      if (nodeA.mSpeed.x > 0 && nodeA.mSpeed.y > 0) {          // Node A down and right
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else {                                               // Node B down and right
          nodeB.mSpeed.x *= -1;
          nodeA.mSpeed.x *= 2;
        }
      } else if (nodeA.mSpeed.x > 0 && nodeA.mSpeed.y < 0) {   // Node A up and right
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= 2;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else {                                               // Node B down and right
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        }
      } else if (nodeA.mSpeed.x < 0 && nodeA.mSpeed.y > 0) {   // Node A down and left
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeB.mSpeed.x *= -1;
          nodeA.mSpeed.x *= 2;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else {                                               // Node B down and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        }
      } else {                                                 // Node A up and left
        if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y > 0) {        // Node B down and left
          nodeA.mSpeed.y *= -1;
          nodeB.mSpeed.y *= -1;
        } else if (nodeB.mSpeed.x > 0 && nodeB.mSpeed.y < 0) { // Node B up and right
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= -1;
        } else if (nodeB.mSpeed.x < 0 && nodeB.mSpeed.y < 0) { // Node B up and left
          nodeA.mSpeed.x *= -1;
          nodeB.mSpeed.x *= 2;
        } else {                                               // Node B down and right
          nodeA.mSpeed.negate();
          nodeB.mSpeed.negate();
        }
      }
      while (distance <= (nodeA.mass + nodeB.mass)) {
        applySpeedTo(nodeA);
        applySpeedTo(nodeB);
        tmp.sub(nodeA.mPosition, nodeB.mPosition);
        distance = Math.sqrt(tmp.lengthSquared());
      }
    }
    /**
     * No collision
     */
    return force;
  }
  
  
  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node [in] Node the node to which the force apply
   * @param force [in] force a force Vector representing the force on a node
   * 
   * TODO: does force should be a property of the node (or not?)
   */
  private void applyForceTo( code_swarm.Node node, Vector2f force )
  {
    double dlen;
    Vector2f mod = new Vector2f(force);

    /**
     * Taken from Newton's 2nd law.  F=ma
     */
    dlen = mod.length();
    if (dlen > 0) {
      mod.scale(node.mass);
      node.mSpeed.add(mod);
    }
  }

  /**
   * Legacy method that apply a force to a node, converting acceleration to speed.
   * 
   * @param node the node to which the force apply
    */
  private void applySpeedTo( code_swarm.Node node )
  {
    // This block enforces a maximum absolute velocity.
    if (node.mSpeed.length() > node.maxSpeed) {
      Vector2f mag = new Vector2f(node.mSpeed.x / node.maxSpeed, node.mSpeed.y / node.maxSpeed);
      node.mSpeed.scale(1/mag.lengthSquared());
    }
    
    // This block convert Speed to Position
    node.mPosition.add(node.mSpeed);
  }
  
  /**
   *  Do nothing.
   */
  public void initializeFrame() {
  }
  
  /**
   *  Do nothing.
   */
  public void finalizeFrame() {
  }
  
  /**
   * Method that allows Physics Engine to modify forces between files and people during the relax stage
   * 
   * @param edge the edge to which the force apply (both ends)
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxEdge(code_swarm.Edge edge) {
    
    if (edge.life <= 0) {
      return;
    }
    //Vector2f force    = new Vector2f();

    // Calculate force between the node "from" and the node "to"
    Vector2f force = calculateForceAlongAnEdge(edge);

    // transmit force projection to file and person nodes
    force.negate();
    applyForceTo(edge.nodeFrom, force); // fNode: attract fNode to pNode
    applySpeedTo(edge.nodeFrom); // fNode: move it.
    //force.negate(); // force is inverted for the other end of the edge: repel pNodes from fNodes
    //applyForceTo(edge.nodeTo, force); // pNode
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param edge the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdateEdge(code_swarm.Edge edge) {
    if (edge.life <= 0) {
      return;
    }
    edge.decay();
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxNode(code_swarm.FileNode fNode ) {
    
    if (fNode.life <= 0) {
      return;
    }
    
    Vector2f forceBetweenFiles = new Vector2f();
    Vector2f forceSummation    = new Vector2f();
      
    // Calculation of repulsive force between persons
    for (int j = 0; j < code_swarm.nodes.size(); j++) {
      code_swarm.FileNode n = (code_swarm.FileNode) code_swarm.nodes.get(j);
      if (n.life <= 0)
        continue;

      if (n != fNode) {
        // elemental force calculation, and summation
        forceBetweenFiles = calculateForceBetweenfNodes(fNode, n);
        forceSummation.add(forceBetweenFiles);
      }
    }
    // Apply repulsive force from other files to this Node
    applyForceTo(fNode, forceSummation);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param fNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdateNode(code_swarm.FileNode fNode) {
    if (fNode.life <= 0) {
      return;
    }
    // Apply Speed to Position on nodes
    applySpeedTo(fNode);
    
    // ensure coherent resulting position
    fNode.mPosition.set(constrain(fNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(fNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    // shortening life
    fNode.decay();
    
    // Apply drag (reduce Speed for next frame calculation)
    fNode.mSpeed.scale(DRAG);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the relax phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onRelaxPerson(code_swarm.PersonNode pNode) {

    if (pNode.life <= 0) {
      return;
    }
    if (pNode.mSpeed.length() == 0) {
      // Range (-1,1)
      pNode.mSpeed.set(pNode.mass*((float)Math.random()-pNode.mass),pNode.mass*((float)Math.random()-pNode.mass));
    }
    
    pNode.mSpeed.scale(pNode.mass);
    pNode.mSpeed.normalize();
    pNode.mSpeed.scale(4);
    
    float distance = pNode.mSpeed.length();
    if (distance > 0) {
      float deltaDistance = (pNode.mass - distance) / (distance * 2);
      deltaDistance *= ((float)pNode.life / pNode.LIFE_INIT);
      
      pNode.mSpeed.scale(deltaDistance);
    }
    
    applySpeedTo(pNode);
  }
  
  /**
   * Method that allows Physics Engine to modify Speed / Position during the update phase.
   * 
   * @param pNode the node to which the force apply
   * 
   * @Note Standard physics is "Position Variation = Speed x Duration" with a convention of "Duration=1" between to frames
   */
  public void onUpdatePerson(code_swarm.PersonNode pNode) {
    if (pNode.life <= 0) {
      return;
    }
    
    // Check for collisions with neighbors.
    for (int i = 0; i < code_swarm.people.size(); i++) {
      if (pNode != code_swarm.people.get(i)) {
        Vector2f force = calculateForceBetweenpNodes(pNode,code_swarm.people.get(i));
        pNode.mPosition.add(force);
      }
    }
    
    // ensure coherent resulting position
    pNode.mPosition.set(constrain(pNode.mPosition.x, 0.0f, (float)code_swarm.width),constrain(pNode.mPosition.y, 0.0f, (float)code_swarm.height));
    
    if ((pNode.mPosition.x < pNode.mass && pNode.mSpeed.x < 0.0f) || (pNode.mPosition.x > (code_swarm.width - pNode.mass) && pNode.mSpeed.x > 0.0f)) {
      // we hit a vertical wall
      pNode.mSpeed.x = -pNode.mSpeed.x;
      while (pNode.mPosition.x < pNode.mass || pNode.mPosition.x > (code_swarm.width - pNode.mass)) {
        pNode.mPosition.x += pNode.mSpeed.x;
      }
    }
    if ((pNode.mPosition.y < pNode.mass && pNode.mSpeed.y < 0.0f) || (pNode.mPosition.y > (code_swarm.height - pNode.mass) && pNode.mSpeed.y > 0.0f)) {
      // we hit a horizontal wall
      pNode.mSpeed.y = -pNode.mSpeed.y;
      while (pNode.mPosition.y < pNode.mass || pNode.mPosition.y > (code_swarm.height - pNode.mass)) {
        pNode.mPosition.y += pNode.mSpeed.y;
      }
    }
    // shortening life
    pNode.decay();
    
    // Apply drag (reduce Speed for next frame calculation)
    pNode.mSpeed.scale(DRAG);
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a Person Node
   */
  public Vector2f pStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width*(float)Math.random(), code_swarm.height*(float)Math.random());
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting location for a File Node
   */
  public Vector2f fStartLocation() {
    Vector2f vec = new Vector2f(code_swarm.width*(float)Math.random(), code_swarm.height*(float)Math.random());
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a Person Node
   */
  public Vector2f pStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
  
  /**
   * 
   * @return Vector2f vector holding the starting velocity for a File Node
   */
  public Vector2f fStartVelocity(float mass) {
    Vector2f vec = new Vector2f(mass*((float)Math.random()*2 - 1), mass*((float)Math.random()*2 -1));
    return vec;
  }
}
