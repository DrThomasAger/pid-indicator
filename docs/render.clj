(import 'com.drthomasager.pid.PidIndicatorOverlay)
(def m (doto (.getDeclaredMethod PidIndicatorOverlay "drawDiamond" (into-array Class [java.awt.Graphics2D Integer/TYPE Integer/TYPE Boolean/TYPE Boolean/TYPE String])) (.setAccessible true)))
(doseq [[file has unk note] [["state-first" true false "goes first"] ["state-second" false false "goes second"]
                             ["state-unknown" false true "attack to find out"] ["state-may-change" true false "goes first, may change"]]]
  (let [w 300 h 120 img (java.awt.image.BufferedImage. w h java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
    (.setRenderingHint g java.awt.RenderingHints/KEY_ANTIALIASING java.awt.RenderingHints/VALUE_ANTIALIAS_ON)
    (.setPaint g (java.awt.GradientPaint. 0 0 (java.awt.Color. 96 104 70) 0 h (java.awt.Color. 70 78 52)))
    (.fill g (java.awt.geom.RoundRectangle2D$Double. 0 0 w h 16 16))
    (.scale g 2.0 2.0)
    (.invoke m nil (object-array [g (int 75) (int 26) (boolean has) (boolean unk) note]))
    (javax.imageio.ImageIO/write img "png" (java.io.File. (str "C:/Users/hanjo/Claude/Projects/pid-indicator/docs/" file ".png")))))
(println "ok")
