package shipwrights.dataplanets.texturing;

import net.minecraft.world.phys.Vec3;

/**
 * Converts cube-face (face, x, y) coordinates into normalised 3D direction vectors
 * so that a single 3D noise field can be sampled seamlessly across all six faces.
 * <p>
 * Face layout (matches common OpenGL convention):
 * 0 = +X  (right)
 * 1 = -X  (left)
 * 2 = +Y  (top)
 * 3 = -Y  (bottom)
 * 4 = +Z  (front)
 * 5 = -Z  (back)
 *
 * @param resolution typically 256
 */
public record CubeMapper(int resolution) {

    /**
     * Return a normalised direction vector for pixel (x, y) on the given face.
     * x and y are in [0, resolution-1].
     */
    public Vec3 toDirection(int face, int x, int y) {
        // Map pixel coordinates to [-1, +1]
        float u = ((x + 0.5f) / resolution) * 2f - 1f;
        float v = ((y + 0.5f) / resolution) * 2f - 1f;

        double dx, dy, dz;

        switch (face) {
            case 0:
                dx = 1;
                dy = v;
                dz = -u;
                break;  // +X
            case 1:
                dx = -1;
                dy = v;
                dz = u;
                break;  // -X
            case 2:
                dx = u;
                dy = 1;
                dz = -v;
                break;  // +Y
            case 3:
                dx = u;
                dy = -1;
                dz = v;
                break;  // -Y
            case 4:
                dx = u;
                dy = v;
                dz = 1;
                break;  // +Z
            case 5:
                dx = -u;
                dy = v;
                dz = -1;
                break;  // -Z
            default:
                throw new IllegalArgumentException("Face index must be 0–5, got: " + face);
        }

        return new Vec3(dx, dy, dz).normalize();
    }
}
