package com.agopinath.butcher;
 
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
 
public class Main extends SimpleApplication {
 
  private BulletAppState bulletAppState;
  private CharacterControl player;
  private Vector3f walkDirection = new Vector3f();
  private boolean left = false, right = false, up = false, down = false;
  private TerrainQuad terrain;
  private RigidBodyControl terrainPhysicsNode;
  
  public static void main(String[] args) {
      Main app = new Main();
      app.start();
  }
 
  public void simpleInitApp() {
      setupPhysics();
      setupWorld();
      setupPlayer();
      
      viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
      flyCam.setMoveSpeed(100);
      setUpKeys();
      setUpLight();   
  }
 
  private void setUpLight() {
      // We add light so we see the scene
      AmbientLight al = new AmbientLight();
      al.setColor(ColorRGBA.White.mult(1.3f));
      rootNode.addLight(al);
   
      DirectionalLight dl = new DirectionalLight();
      dl.setColor(ColorRGBA.White);
      dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
      rootNode.addLight(dl);
  }
 
  private void setUpKeys() {
      inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
      inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
      inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
      inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
      inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
      inputManager.addListener(gameInput, "Left");
      inputManager.addListener(gameInput, "Right");
      inputManager.addListener(gameInput, "Up");
      inputManager.addListener(gameInput, "Down");
      inputManager.addListener(gameInput, "Jump");
  }
 
  private ActionListener gameInput = new ActionListener() {
        public void onAction(String binding, boolean isPressed, float tpf) {
            if (binding.equals("Left")) {
              left = isPressed;
            } else if (binding.equals("Right")) {
              right = isPressed;
            } else if (binding.equals("Up")) {
              up = isPressed;
            } else if (binding.equals("Down")) {
              down = isPressed;
            } else if (binding.equals("Jump")) {
              player.jump();
            }
        }
    };
 
  @Override
  public void simpleUpdate(float tpf) {
      Vector3f camDir = cam.getDirection().clone().multLocal(0.6f);
      Vector3f camLeft = cam.getLeft().clone().multLocal(0.4f);
      walkDirection.set(0, 0, 0);
      
      if (left)  { walkDirection.addLocal(camLeft); }
      if (right) { walkDirection.addLocal(camLeft.negate()); }
      if (up)    { walkDirection.addLocal(camDir); }
      if (down)  { walkDirection.addLocal(camDir.negate()); }
      
      player.setWalkDirection(walkDirection);
      cam.setLocation(player.getPhysicsLocation());
  }

   private void setupPhysics() { 
       bulletAppState = new BulletAppState();
       bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
       //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
       stateManager.attach(bulletAppState);
    }
    
   private void setupWorld() {
        Material mat_terrain = new Material(assetManager, 
                "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        mat_terrain.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        Texture grass = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex1", grass);
        mat_terrain.setFloat("Tex1Scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        Texture dirt = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex2", dirt);
        mat_terrain.setFloat("Tex2Scale", 32f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        mat_terrain.setTexture("Tex3", rock);
        mat_terrain.setFloat("Tex3Scale", 128f);

        /** 2. Create the height map */
        AbstractHeightMap heightmap = null;
        Texture heightMapImage = assetManager.loadTexture(
                "Textures/heightmap_textures/map1.gif");

        heightmap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightmap.load();

        int patchSize = 65;
        terrain = new TerrainQuad("mountainousTerrain", patchSize, 513, heightmap.getHeightMap());
 
        terrain.setMaterial(mat_terrain);
        terrain.setLocalTranslation(0, -100f, 0);
        terrain.setLocalScale(8f, 4f, 8f);
        
        Node terrainScene = (Node) assetManager.loadModel("Textures/Map1.j3o");
        terrainPhysicsNode = new RigidBodyControl(CollisionShapeFactory.createMeshShape(terrainScene), 0);
        terrainScene.addControl(terrainPhysicsNode);
        
        TerrainLodControl lodControl = terrainScene.getControl(TerrainLodControl.class);
            if (lodControl != null)
                lodControl.setCamera(getCamera()); 
        
        rootNode.attachChild(terrainScene);
        bulletAppState.getPhysicsSpace().add(terrainPhysicsNode);
    }
    
    private void setupPlayer() {
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 6f, 1);
        
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setJumpSpeed(40);
        player.setFallSpeed(30);
        player.setGravity(35);
        player.setPhysicsLocation(new Vector3f(0, 50f, 0));

        bulletAppState.getPhysicsSpace().add(player);   
    }
}
