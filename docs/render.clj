(import 'com.drthomasager.pid.PidIndicatorOverlay)
(def sc (Class/forName "com.drthomasager.pid.PidIndicatorOverlay$State"))
(defn st [n] (Enum/valueOf sc n))
(def m (doto (.getDeclaredMethod PidIndicatorOverlay "drawDiamond" (into-array Class [java.awt.Graphics2D Integer/TYPE Integer/TYPE sc Float/TYPE])) (.setAccessible true)))
(defn bg [g w h] (.setPaint g (java.awt.GradientPaint. 0 0 (java.awt.Color. 96 104 70) 0 h (java.awt.Color. 70 78 52))) (.fill g (java.awt.geom.RoundRectangle2D$Double. 0 0 w h 16 16)))
(def dir "C:/Users/hanjo/Claude/Projects/pid-indicator/docs/")
(doseq [[file st pulse] [["state-first" (st "FIRST") 1.0] ["state-second" (st "SECOND") 1.0]
                         ["state-unknown" (st "UNKNOWN") 1.0] ["state-may-change" (st "FIRST") 0.35]]]
  (let [w 180 h 100 img (java.awt.image.BufferedImage. w h java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
    (bg g w h) (.scale g 2.0 2.0)
    (.invoke m nil (object-array [g (int 45) (int 28) st (float pulse)]))
    (javax.imageio.ImageIO/write img "png" (java.io.File. (str dir file ".png")))))
(let [w 1040 h 260 img (java.awt.image.BufferedImage. w h java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
  (bg g w h) (.scale g 2.0 2.0)
  (doseq [[x st pulse title] [[70 (st "FIRST") 1.0 "Goes first"] [200 (st "SECOND") 1.0 "Goes second"]
                              [330 (st "UNKNOWN") 1.0 "Not known yet"] [450 (st "FIRST") 0.35 "May change"]]]
    (.invoke m nil (object-array [g (int x) (int 50) st (float pulse)]))
    (.setRenderingHint g java.awt.RenderingHints/KEY_TEXT_ANTIALIASING java.awt.RenderingHints/VALUE_TEXT_ANTIALIAS_OFF)
    (.setFont g (net.runelite.client.ui.FontManager/getRunescapeBoldFont))
    (let [tw (.stringWidth (.getFontMetrics g) title) tx (int (- x (/ tw 2)))]
      (.setColor g java.awt.Color/BLACK) (.drawString g title (inc tx) 106)
      (.setColor g (java.awt.Color. 255 152 31)) (.drawString g title tx 105)))
  (javax.imageio.ImageIO/write img "png" (java.io.File. (str dir "diamonds.png"))))
(let [img (java.awt.image.BufferedImage. 48 48 java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
  (.scale g 1.6 1.6)
  (.invoke m nil (object-array [g (int 15) (int 17) (st "FIRST") (float 1.0)]))
  (javax.imageio.ImageIO/write img "png" (java.io.File. "C:/Users/hanjo/Claude/Projects/pid-indicator/icon.png")))
(println "ok")
