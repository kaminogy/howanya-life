import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Generates the "Howanya" sprite frames into app/src/main/assets/cat/.
 *
 * Run from the project root:
 *   "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" -Djava.awt.headless=true tools/HowanyaSprites.java
 *
 * Frames are 256x256 transparent PNGs with the feet at y ~= 236 (see ASSETS.md).
 * The character faces the viewer, so assets/cat/settings.properties turns mirroring off.
 *
 * Shapes are laid out in "reference units", measured from the official front-view artwork:
 * the ear tips sit at y = 0, the ground at y = 263, and the face is centred on x = 128.
 * Each frame is then scaled down to fit the 256px canvas.
 */
public class HowanyaSprites {

    static final int SIZE = 256;
    static final double GROUND = 263;
    static final double SCALE = 228 / GROUND;
    static final double FEET_Y = 236;

    static final Color GRAY = new Color(0xDCDEE3);
    static final Color BLUE = new Color(0x0A9BF5);
    static final Color INK = new Color(0x141414);
    static final Color WHITE = Color.WHITE;
    static final Color TEAR = new Color(0x9ADCFF);
    static final Color MOUTH = new Color(0x7A1F2B);
    static final Color TONGUE = new Color(0xF28CA0);
    static final Color DONUT = new Color(0xD9A066);
    static final Color ICING = new Color(0xF7A8C4);

    static BasicStroke stroke(double width) {
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    static final BasicStroke OUTLINE = stroke(7);
    static final BasicStroke INNER_LINE = stroke(5.5);
    static final BasicStroke EYE_OUTLINE = stroke(4.5);
    static final BasicStroke EYE_INNER = stroke(3);
    static final BasicStroke LASH = stroke(3.4);
    static final BasicStroke FACE_LINE = stroke(3.6);

    enum Eyes { OPEN, HAPPY, CLOSED, SLEEPY, TEARS }
    enum Mouth { OMEGA, OPEN, SAD, YAWN, CHOMP }

    /** Everything that changes between frames of a standing pose. */
    static class Pose {
        double bob;                 // vertical shift of the whole body in canvas px, negative = up
        double shakeX;              // horizontal shift in canvas px
        double headTilt;            // degrees, positive = clockwise
        boolean leftFootForward = true;
        double stepL, stepR;        // how far each foot is lifted
        double armL, armR;          // arm raise in degrees, 0 = hanging down
        double armLength = 44;
        double footWidth = 66;      // narrower feet for walking; the sitting pose uses the full size
        double tail;                // tail sway in degrees
        double stretch;             // how far the torso stretches upward
        Eyes eyes = Eyes.OPEN;
        Mouth mouth = Mouth.OMEGA;
        int tearFrame = -1;
        boolean donut;
    }

    public static void main(String[] args) throws IOException {
        File root = new File(args.length > 0 ? args[0] : "app/src/main/assets/cat");

        write(root, "idle", 4, f -> {
            Pose p = new Pose();
            p.bob = f % 2 == 0 ? 0 : -1.5;
            p.tail = new double[] {-6, 0, 6, 0}[f];
            p.headTilt = new double[] {0, 2, 0, -2}[f];
            return standing(p);
        });
        write(root, "walk", 4, f -> {
            Pose p = new Pose();
            p.footWidth = 46;
            p.leftFootForward = f < 2;
            p.stepR = f == 0 ? 10 : 0;
            p.stepL = f == 2 ? 10 : 0;
            p.bob = f % 2 == 0 ? 0 : -3;
            p.armL = f < 2 ? 4 : 18;
            p.armR = f < 2 ? 18 : 4;
            p.tail = new double[] {-8, 0, 8, 0}[f];
            p.headTilt = new double[] {3, 0, -3, 0}[f];
            return standing(p);
        });
        write(root, "run", 4, f -> {
            Pose p = new Pose();
            p.footWidth = 46;
            p.leftFootForward = f < 2;
            p.stepR = f == 0 ? 18 : 6;
            p.stepL = f == 2 ? 18 : 6;
            p.bob = f % 2 == 0 ? -9 : -3;
            p.armL = f < 2 ? 10 : 55;
            p.armR = f < 2 ? 55 : 10;
            p.tail = new double[] {-16, 0, 16, 0}[f];
            p.headTilt = new double[] {5, 0, -5, 0}[f];
            return standing(p);
        });
        write(root, "stretch", 4, f -> {
            Pose p = new Pose();
            p.armL = p.armR = new double[] {0, 70, 100, 108}[f];
            p.armLength = f >= 2 ? 50 : 44;
            p.stretch = new double[] {0, 3, 7, 9}[f];
            p.eyes = f >= 2 ? Eyes.HAPPY : Eyes.OPEN;
            p.mouth = f == 3 ? Mouth.YAWN : Mouth.OMEGA;
            return standing(p);
        });
        write(root, "laugh", 4, f -> {
            Pose p = new Pose();
            p.eyes = Eyes.HAPPY;
            p.mouth = Mouth.OPEN;
            p.bob = f % 2 == 0 ? 0 : -8;
            p.armL = f % 2 == 0 ? 40 : 65;
            p.armR = f % 2 == 0 ? 65 : 40;
            p.headTilt = new double[] {-4, 0, 4, 0}[f];
            p.tail = new double[] {-10, 0, 10, 0}[f];
            return standing(p);
        });
        write(root, "cry", 4, f -> {
            Pose p = new Pose();
            p.eyes = Eyes.TEARS;
            p.tearFrame = f;
            p.mouth = Mouth.SAD;
            p.shakeX = new double[] {0, -2, 0, 2}[f];
            p.headTilt = 4;
            p.tail = 12;
            return standing(p);
        });
        write(root, "eat", 4, f -> {
            Pose p = new Pose();
            p.donut = true;
            p.armL = p.armR = 34;
            p.armLength = 40;
            p.mouth = f % 2 == 0 ? Mouth.CHOMP : Mouth.OMEGA;
            p.eyes = f % 2 == 0 ? Eyes.OPEN : Eyes.HAPPY;
            p.bob = f % 2 == 0 ? 0 : -1.5;
            p.headTilt = new double[] {2, 0, -2, 0}[f];
            return standing(p);
        });
        write(root, "sleep", 4, HowanyaSprites::sleeping);
        writeContactSheet(root, new File(args.length > 1 ? args[1] : "build/howanya-sheet.png"));
        System.out.println("Wrote sprites to " + root.getPath());
    }

    static final String[] ACTIONS = {"idle", "walk", "run", "sleep", "stretch", "laugh", "cry", "eat"};

    /** All frames in one image (one row per action) for checking the artwork at a glance. */
    static void writeContactSheet(File root, File out) throws IOException {
        int cell = SIZE / 2;
        BufferedImage sheet = new BufferedImage(cell * 4, cell * ACTIONS.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas(sheet);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        for (int row = 0; row < ACTIONS.length; row++) {
            for (int col = 0; col < 4; col++) {
                g.setColor((row + col) % 2 == 0 ? new Color(0x2B3A67) : new Color(0x8FD3F4));
                g.fillRect(col * cell, row * cell, cell, cell);
                File frame = new File(root, String.format("%s/%02d.png", ACTIONS[row], col + 1));
                g.drawImage(ImageIO.read(frame), col * cell, row * cell, cell, cell, null);
            }
        }
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
    }

    interface FrameMaker { BufferedImage make(int frame); }

    static void write(File root, String action, int frames, FrameMaker maker) throws IOException {
        File dir = new File(root, action);
        dir.mkdirs();
        for (int f = 0; f < frames; f++) {
            ImageIO.write(maker.make(f), "png", new File(dir, String.format("%02d.png", f + 1)));
        }
    }

    /** A canvas already transformed so drawing happens in reference units. */
    static Graphics2D canvas(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    static Graphics2D referenceCanvas(BufferedImage image, double shakeX, double bob) {
        Graphics2D g = canvas(image);
        g.translate(SIZE / 2.0 + shakeX, FEET_Y + bob);
        g.scale(SCALE, SCALE);
        g.translate(-128, -GROUND);
        return g;
    }

    static Area area(Shape shape) {
        return new Area(shape);
    }

    static Area transformed(Shape shape, AffineTransform t) {
        return new Area(t.createTransformedShape(shape));
    }

    // ---------------------------------------------------------------- standing

    /**
     * The whole body is one silhouette with a single outer outline, like the official art.
     * Arms, legs and the neck are only hinted at with short lines inside the silhouette.
     */
    static BufferedImage standing(Pose p) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = referenceCanvas(image, p.shakeX, p.bob);

        AffineTransform headT = new AffineTransform();
        headT.translate(0, -p.stretch);
        headT.rotate(Math.toRadians(p.headTilt), 128, 168);

        Area head = transformed(headShape(), headT);
        Area torso = area(torsoShape(p.stretch));

        // Official sitting pose: two big rounded legs pointing to the front left, one set
        // higher and further out, the other lower and in front. Walking mirrors it.
        double fw = p.footWidth;
        double backX = 63 + (66 - fw);
        double frontX = 100 + (66 - fw) / 2;
        Area back;
        Area front;
        if (p.leftFootForward) {
            back = area(new Ellipse2D.Double(backX, 210 - p.stepL, fw, 40));
            front = area(new Ellipse2D.Double(frontX, 222 - p.stepR, fw, 41));
        } else {
            back = area(new Ellipse2D.Double(SIZE - backX - fw, 210 - p.stepR, fw, 40));
            front = area(new Ellipse2D.Double(SIZE - frontX - fw, 222 - p.stepL, fw, 41));
        }

        // Short, thick arms hanging along the torso; the paws end where the legs begin.
        double shoulderY = 170 - p.stretch;
        Area armL = arm(SHOULDER_L, shoulderY, ARM_SPREAD + p.armL, p.armLength);
        Area armR = arm(SHOULDER_R, shoulderY, -(ARM_SPREAD + p.armR), p.armLength);

        Area body = new Area(torso);
        body.add(front);
        body.add(back);
        body.add(armL);
        body.add(armR);
        Area silhouette = new Area(body);
        silhouette.add(head);

        drawTail(g, p.tail);

        g.setColor(GRAY);
        g.fill(silhouette);
        Area patch = transformed(patchShape(), headT);
        patch.intersect(head);
        g.setColor(BLUE);
        g.fill(patch);
        g.fill(new Ellipse2D.Double(113, 172 - p.stretch * 0.5, 30, 44));

        g.setColor(INK);
        g.setStroke(OUTLINE);
        g.draw(silhouette);

        // Inner lines.
        g.setStroke(INNER_LINE);
        Area neckSides = area(new Rectangle2D.Double(0, 0, 106, SIZE * 2));
        neckSides.add(area(new Rectangle2D.Double(150, 0, SIZE, SIZE * 2)));
        Area upperBody = new Area(torso);
        upperBody.add(armL);
        upperBody.add(armR);
        Area neckClip = new Area(upperBody);
        neckClip.intersect(neckSides);
        drawClipped(g, transformed(headOutline(), headT), neckClip);
        // Only a short line along the top of the front leg; the back leg gets none.
        Area thighLine = new Area(upperBody);
        thighLine.add(back);
        thighLine.intersect(area(p.leftFootForward
                ? new Rectangle2D.Double(96, 200, 40, 45)
                : new Rectangle2D.Double(120, 200, 40, 45)));
        drawClipped(g, front, thighLine);
        Area inside = new Area(torso);
        inside.add(head);
        Area armLClip = armInnerSide(SHOULDER_L, shoulderY, ARM_SPREAD + p.armL, p.armLength, true);
        armLClip.intersect(inside);
        drawClipped(g, armL, armLClip);
        Area armRClip = armInnerSide(SHOULDER_R, shoulderY, -(ARM_SPREAD + p.armR), p.armLength, false);
        armRClip.intersect(inside);
        drawClipped(g, armR, armRClip);

        if (p.donut) {
            drawDonut(g, 128, 196 - p.stretch);
            // Paws on top of the donut.
            for (Area a : new Area[] {armL, armR}) {
                Area paw = new Area(a);
                paw.intersect(area(new Rectangle2D.Double(0, 184, SIZE, 40)));
                g.setColor(GRAY);
                g.fill(paw);
                g.setColor(INK);
                g.setStroke(INNER_LINE);
                g.draw(paw);
            }
        }

        AffineTransform beforeFace = g.getTransform();
        g.transform(headT);
        drawFace(g, p.eyes, p.mouth, p.tearFrame);
        g.setTransform(beforeFace);

        g.dispose();
        return image;
    }

    static final double SHOULDER_L = 100;
    static final double SHOULDER_R = 156;
    /** How far the arms hang away from the body at rest, in degrees. */
    static final double ARM_SPREAD = 16;

    /** Narrow torso between the arms; its bottom is hidden by the legs. */
    static Shape torsoShape(double stretch) {
        double top = 148 - stretch;
        Path2D torso = new Path2D.Double();
        torso.moveTo(100, top);
        torso.lineTo(156, top);
        torso.curveTo(164, 180, 166, 215, 160, 240);
        torso.lineTo(96, 240);
        torso.curveTo(90, 215, 92, 180, 100, top);
        torso.closePath();
        return torso;
    }

    static Area arm(double x, double y, double degrees, double length) {
        Shape shape = new RoundRectangle2D.Double(x - 14, y - 6, 28, length, 26, 26);
        return transformed(shape, AffineTransform.getRotateInstance(Math.toRadians(degrees), x, y));
    }

    /** The belly-facing half of an arm, below the armpit: only that edge is drawn as a line. */
    static Area armInnerSide(double x, double y, double degrees, double length, boolean left) {
        Shape half = new Rectangle2D.Double(left ? x : x - 20, y + 10, 20, length + 10);
        return transformed(half, AffineTransform.getRotateInstance(Math.toRadians(degrees), x, y));
    }

    static void drawClipped(Graphics2D g, Shape shape, Shape clip) {
        Shape old = g.getClip();
        g.clip(clip);
        g.draw(shape);
        g.setClip(old);
    }

    /** Fully blue tail that leaves the right hip, runs right and hooks downward. */
    static void drawTail(Graphics2D g, double sway) {
        AffineTransform saved = g.getTransform();
        g.rotate(Math.toRadians(sway), 166, 228);
        Path2D tail = new Path2D.Double();
        tail.moveTo(160, 230);
        tail.curveTo(196, 214, 222, 226, 216, 248);
        tail.quadTo(213, 259, 204, 257);
        g.setColor(INK);
        g.setStroke(stroke(19));
        g.draw(tail);
        g.setColor(BLUE);
        g.setStroke(stroke(11));
        g.draw(tail);
        g.setTransform(saved);
    }

    static void drawDonut(Graphics2D g, double cx, double cy) {
        Area ring = area(new Ellipse2D.Double(cx - 25, cy - 18, 50, 36));
        ring.subtract(area(new Ellipse2D.Double(cx - 8, cy - 6, 16, 12)));
        ring.subtract(area(new Ellipse2D.Double(cx + 12, cy - 28, 20, 20)));
        g.setColor(DONUT);
        g.fill(ring);
        Area icing = area(new Ellipse2D.Double(cx - 20, cy - 15, 40, 25));
        icing.subtract(area(new Ellipse2D.Double(cx - 10, cy - 8, 20, 15)));
        icing.subtract(area(new Ellipse2D.Double(cx + 10, cy - 30, 24, 24)));
        g.setColor(ICING);
        g.fill(icing);
        g.setColor(INK);
        g.setStroke(INNER_LINE);
        g.draw(ring);
    }

    // ---------------------------------------------------------------- head

    static Shape headOutline() {
        return new Ellipse2D.Double(45, 52, 166, 116);
    }

    static Shape leftEar() {
        Path2D ear = new Path2D.Double();
        ear.moveTo(70, 90);
        ear.curveTo(68, 42, 82, 2, 99, 2);
        ear.curveTo(114, 2, 121, 32, 126, 64);
        ear.closePath();
        return ear;
    }

    static Area headShape() {
        Area head = area(headOutline());
        Shape ear = leftEar();
        head.add(area(ear));
        head.add(transformed(ear, new AffineTransform(-1, 0, 0, 1, SIZE, 0)));
        return head;
    }

    /** Blue over the left ear and top-left of the head, ending beside the left eye. */
    static Shape patchShape() {
        Path2D patch = new Path2D.Double();
        patch.moveTo(0, -20);
        patch.lineTo(134, -20);
        patch.lineTo(134, 54);
        patch.curveTo(142, 74, 130, 104, 106, 118);
        patch.lineTo(40, 126);
        patch.lineTo(0, 126);
        patch.closePath();
        return patch;
    }

    static final double EYE_LEFT_X = 95;
    static final double EYE_RIGHT_X = 161;
    static final double EYE_Y = 112;
    static final double EYE_W = 26;
    static final double EYE_H = 46;

    static void drawFace(Graphics2D g, Eyes eyes, Mouth mouth, int tearFrame) {
        drawEye(g, EYE_LEFT_X, EYE_Y, eyes, tearFrame);
        drawEye(g, EYE_RIGHT_X, EYE_Y, eyes, tearFrame);
        g.setColor(INK);
        g.fill(new Ellipse2D.Double(120, 120, 15, 9));
        drawMouth(g, mouth);
    }

    static void drawEye(Graphics2D g, double cx, double cy, Eyes eyes, int tearFrame) {
        double hw = EYE_W / 2;
        double hh = EYE_H / 2;
        // Where the eyelash starts: just off the upper-right of the eye.
        double lashX;
        double lashY;
        g.setColor(INK);
        g.setStroke(EYE_OUTLINE);
        switch (eyes) {
            case HAPPY -> {
                Path2D arc = new Path2D.Double();
                arc.moveTo(cx - hw, cy + 6);
                arc.quadTo(cx, cy - 18, cx + hw, cy + 6);
                g.draw(arc);
                lashX = cx + hw - 2;
                lashY = cy - 4;
            }
            case CLOSED -> {
                Path2D arc = new Path2D.Double();
                arc.moveTo(cx - hw, cy);
                arc.quadTo(cx, cy + 13, cx + hw, cy);
                g.draw(arc);
                lashX = cx + hw;
                lashY = cy - 1;
            }
            case SLEEPY -> {
                // Drowsy, half-closed: a white oval with the eyelid line across the middle.
                Ellipse2D outer = new Ellipse2D.Double(cx - hw, cy - hh, EYE_W, EYE_H);
                g.setColor(WHITE);
                g.fill(outer);
                Path2D lid = new Path2D.Double();
                lid.moveTo(cx - hw, cy - 2);
                lid.quadTo(cx, cy + 4, cx + hw, cy - 2);
                g.setColor(INK);
                g.setStroke(EYE_INNER);
                drawClipped(g, lid, outer);
                g.setStroke(EYE_OUTLINE);
                g.draw(outer);
                lashX = cx + hw * 0.95;
                lashY = cy - hh * 0.58;
            }
            default -> {
                // Upright oval, mostly blue, with a white crescent along the lower right
                // separated from the blue by a thin black line.
                Ellipse2D outer = new Ellipse2D.Double(cx - hw, cy - hh, EYE_W, EYE_H);
                Ellipse2D iris = new Ellipse2D.Double(cx - hw - 1.5, cy - hh - 2, EYE_W - 5, EYE_H - 4);
                g.setColor(BLUE);
                g.fill(outer);
                Area crescent = area(outer);
                crescent.subtract(area(iris));
                g.setColor(WHITE);
                g.fill(crescent);
                g.setColor(INK);
                g.setStroke(EYE_INNER);
                drawClipped(g, iris, outer);
                g.setStroke(EYE_OUTLINE);
                g.draw(outer);
                lashX = cx + hw * 0.95;
                lashY = cy - hh * 0.58;
            }
        }

        // Short lash rising up and to the right, curling slightly at the tip.
        Path2D lash = new Path2D.Double();
        lash.moveTo(lashX, lashY);
        lash.quadTo(lashX + 7, lashY - 1, lashX + 9.5, lashY - 8);
        g.setColor(INK);
        g.setStroke(LASH);
        g.draw(lash);

        if (eyes == Eyes.TEARS && tearFrame >= 0) {
            double y = cy + hh + tearFrame * 9;
            Path2D drop = new Path2D.Double();
            drop.moveTo(cx, y);
            drop.curveTo(cx + 9, y + 10, cx + 7, y + 18, cx, y + 18);
            drop.curveTo(cx - 7, y + 18, cx - 9, y + 10, cx, y);
            g.setColor(TEAR);
            g.fill(drop);
            g.setColor(INK);
            g.setStroke(stroke(2));
            g.draw(drop);
        }
    }

    static void drawMouth(Graphics2D g, Mouth mouth) {
        g.setColor(INK);
        g.setStroke(FACE_LINE);
        switch (mouth) {
            case OMEGA -> {
                Path2D m = new Path2D.Double();
                m.moveTo(104, 130);
                m.quadTo(106, 142, 116, 141);
                m.quadTo(127, 140, 128, 133);
                m.quadTo(129, 140, 140, 141);
                m.quadTo(150, 142, 152, 130);
                g.draw(m);
            }
            case OPEN -> {
                Path2D m = new Path2D.Double();
                m.moveTo(108, 133);
                m.quadTo(128, 137, 148, 133);
                m.quadTo(145, 158, 128, 158);
                m.quadTo(111, 158, 108, 133);
                m.closePath();
                g.setColor(MOUTH);
                g.fill(m);
                g.setColor(TONGUE);
                g.fill(new Ellipse2D.Double(118, 146, 20, 11));
                g.setColor(INK);
                g.draw(m);
            }
            case SAD -> {
                Path2D m = new Path2D.Double();
                m.moveTo(112, 145);
                m.quadTo(128, 131, 144, 145);
                g.draw(m);
            }
            case YAWN -> {
                Ellipse2D m = new Ellipse2D.Double(119, 132, 18, 22);
                g.setColor(MOUTH);
                g.fill(m);
                g.setColor(INK);
                g.draw(m);
            }
            case CHOMP -> {
                Ellipse2D m = new Ellipse2D.Double(118, 133, 20, 13);
                g.setColor(MOUTH);
                g.fill(m);
                g.setColor(INK);
                g.draw(m);
            }
        }
    }

    // ---------------------------------------------------------------- sleeping

    /**
     * The official "taking a rest" pose: crouched with the head low in front, the back end
     * raised high behind it to the upper left with the tail standing up, drowsy half-closed
     * eyes, and two small front paws under the chin.
     */
    static BufferedImage sleeping(int frame) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = referenceCanvas(image, 0, 0);
        double breath = frame % 2 == 0 ? 0 : 2.5;

        AffineTransform headT = new AffineTransform();
        headT.translate(22, 59);
        headT.rotate(Math.toRadians(-8), 128, 110);

        Area head = transformed(headShape(), headT);
        Area back = area(new Ellipse2D.Double(39, 48 - breath, 98, 136 + breath));
        Area pawL = area(new RoundRectangle2D.Double(111, 214, 44, 49, 30, 30));
        Area pawR = area(new RoundRectangle2D.Double(171, 214, 41, 33, 30, 30));
        Area silhouette = new Area(back);
        silhouette.add(pawL);
        silhouette.add(pawR);
        silhouette.add(head);

        drawRaisedTail(g, breath);

        g.setColor(GRAY);
        g.fill(silhouette);
        Area patch = transformed(patchShape(), headT);
        patch.intersect(head);
        g.setColor(BLUE);
        g.fill(patch);
        g.setColor(INK);
        g.setStroke(OUTLINE);
        g.draw(silhouette);

        // The head is in front of the body and the paws, so its outline runs over them.
        g.setStroke(INNER_LINE);
        Area behindHead = new Area(back);
        behindHead.add(pawL);
        behindHead.add(pawR);
        drawClipped(g, transformed(headShape(), headT), behindHead);
        // A single line for the front edge of the hind leg.
        Path2D hindLeg = new Path2D.Double();
        hindLeg.moveTo(52, 122);
        hindLeg.quadTo(46, 148, 54, 172);
        g.draw(hindLeg);

        AffineTransform beforeFace = g.getTransform();
        g.transform(headT);
        drawFace(g, Eyes.SLEEPY, Mouth.OMEGA, -1);
        g.setTransform(beforeFace);

        drawZ(g, 212 + frame * 2, 70 - frame * 8, 26 + frame);
        g.dispose();
        return image;
    }

    /** Fully blue tail standing up from the top of the raised back, curling over at the tip. */
    static void drawRaisedTail(Graphics2D g, double breath) {
        Path2D tail = new Path2D.Double();
        tail.moveTo(60, 72 - breath);
        tail.curveTo(48, 56, 40, 36, 46, 26);
        tail.curveTo(50, 17, 62, 16, 66, 25);
        g.setColor(INK);
        g.setStroke(stroke(19));
        g.draw(tail);
        g.setColor(BLUE);
        g.setStroke(stroke(11));
        g.draw(tail);
    }

    static void drawZ(Graphics2D g, double x, double y, float size) {
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout("z", new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size)), frc);
        Shape glyph = layout.getOutline(AffineTransform.getTranslateInstance(x, y));
        g.setColor(INK);
        g.setStroke(stroke(4));
        g.draw(glyph);
        g.setColor(WHITE);
        g.fill(glyph);
    }
}
