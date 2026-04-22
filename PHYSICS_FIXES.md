# Physics Bug Fixes

**Author:** loki_boba  
**Discord:** loki_boba  
**Date:** 2026-04-22

## Fixed Bugs

### 1. Infinite thrust from magnets on same sublevel

**Problem:**  
When two magnets were placed on the same physical construction (e.g., front of vehicle and on crane attached to same vehicle), they created infinite thrust. The code applied linear forces to both magnets without accounting that these are internal forces of one rigid body, which should cancel out according to Newton's third law.

**Solution:**  
Changed force application logic in `MagnetPair.java`. Now:
- Detects if magnets are on the same sublevel (line 116)
- For magnets on same sublevel, only applies **torque** (rotational force), not linear force (lines 192-195, 204-207)
- Magnets on same object can rotate it, but not propel it

```java
// Check if magnets are on the same rigid body
final boolean sameSublevel = (subLevel1 != null && subLevel1 == subLevel2);

// For magnets on same sublevel, only apply torque (internal forces cancel out)
if (!sameSublevel) {
    forceTotal1.applyAndRecordPointForce(...);
}
forceTotal1.getForceTotal().applyLinearAndAngularImpulse(new Vector3d(), this.totalTorque1.mul(timeStep));
```

**File:** `simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/redstone_magnet/MagnetPair.java`

---

### 2. Infinite impulse from springs with mismatched sizes

**Problem:**  
When connecting three blocks with two springs of different thickness (thick → thin → thick) between physical objects, infinite impulse occurred. The construction started flying very fast and teleporting even after removing one spring.

**Solution:**  
Applied two fixes in `SpringBlockEntity.java`:

1. **Force clamping** (lines 240-245):
   ```java
   // Clamp force to prevent infinite impulse with mismatched spring sizes
   final double maxForce = 1000.0 * sizeScale;
   if (hookesPointForce.lengthSquared() > maxForce * maxForce) {
       hookesPointForce.normalize().mul(maxForce);
   }
   ```

2. **Increased damping** (line 204):
   ```java
   // Increased damping to prevent oscillations with mismatched spring sizes
   dampingPointForce.mul(-6.5); // was -4.5
   ```

**File:** `simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/spring/SpringBlockEntity.java`

---

## Technical Details

### Magnets
- Magnets on same sublevel create only rotational moment (physically correct)
- Magnets on different sublevels apply full forces (linear + rotational)
- Preserves realistic behavior: magnets can rotate construction but not propel it
- Minimal performance impact (one boolean check)

### Springs
- Maximum force scales with spring size (LARGE: 8000, MEDIUM: 1000, SMALL: 500)
- Damping increased from -4.5 to -6.5 for better stabilization
- Maintains physical correctness for normal configurations

## Testing

Recommended tests:
1. Magnets on same construction (should rotate but not propel the construction)
2. Magnets on different constructions (should work normally - attract/repel)
3. Springs with different thickness between physical blocks (should not create infinite impulse)
4. Normal spring configurations (should work as before)
