(import 'com.drthomasager.pid.PidIndicatorOverlay)
(def sc (Class/forName "com.drthomasager.pid.PidIndicatorOverlay$State"))
(defn st [n] (Enum/valueOf sc n))
(def stone (javax.imageio.ImageIO/read (java.io.File. "C:/Users/hanjo/Claude/Projects/pid-indicator/.testhome/spritedump/out/1030-0.png")))
(def tint (doto (.getDeclaredMethod PidIndicatorOverlay "tint" (into-array Class [java.awt.image.BufferedImage sc])) (.setAccessible true)))
(def draw (doto (.getDeclaredMethod PidIndicatorOverlay "drawDiamond" (into-array Class [java.awt.Graphics2D java.awt.image.BufferedImage Integer/TYPE Integer/TYPE Integer/TYPE sc Boolean/TYPE])) (.setAccessible true)))
(defn tex [s] (.invoke tint nil (object-array [stone (st s)])))
(defn bg [g w h] (.setPaint g (java.awt.GradientPaint. 0 0 (java.awt.Color. 96 104 70) 0 h (java.awt.Color. 70 78 52))) (.fill g (java.awt.geom.RoundRectangle2D$Double. 0 0 w h 16 16)))
(defn bar [g x y] (.setColor g java.awt.Color/BLACK) (.fillRect g (- x 16) (- y 3) 32 6) (.setColor g (java.awt.Color. 0 200 0)) (.fillRect g (- x 15) (- y 2) 20 4) (.setColor g (java.awt.Color. 200 0 0)) (.fillRect g (+ x 5) (- y 2) 10 4))
(defn one [g cx cy s may] (bar g cx cy) (let [r (if (= s "FIRST") 10 8)] (.invoke draw nil (object-array [g (tex s) (int (+ cx 19 r)) (int cy) (int r) (st s) (boolean may)]))))
(def dir "C:/Users/hanjo/Claude/Projects/pid-indicator/docs/")
(doseq [[file s may] [["state-first" "FIRST" false] ["state-second" "SECOND" false] ["state-unknown" "UNKNOWN" false] ["state-may-change" "FIRST" true]]]
  (let [w 180 h 80 img (java.awt.image.BufferedImage. w h java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
    (bg g w h) (.scale g 2.0 2.0) (one g 30 20 s may)
    (javax.imageio.ImageIO/write img "png" (java.io.File. (str dir file ".png")))))
(let [w 1040 h 200 img (java.awt.image.BufferedImage. w h java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
  (bg g w h) (.scale g 2.0 2.0)
  (doseq [[x s may title] [[50 "FIRST" false "Goes first"] [180 "SECOND" false "Goes second"] [310 "UNKNOWN" false "Not known yet"] [440 "FIRST" true "May change"]]]
    (one g x 40 s may)
    (.setRenderingHint g java.awt.RenderingHints/KEY_TEXT_ANTIALIASING java.awt.RenderingHints/VALUE_TEXT_ANTIALIAS_OFF)
    (.setFont g (net.runelite.client.ui.FontManager/getRunescapeBoldFont))
    (let [tw (.stringWidth (.getFontMetrics g) title) tx (int (- (+ x 10) (/ tw 2)))]
      (.setColor g java.awt.Color/BLACK) (.drawString g title (inc tx) 81) (.setColor g (java.awt.Color. 255 152 31)) (.drawString g title tx 80)))
  (javax.imageio.ImageIO/write img "png" (java.io.File. (str dir "diamonds.png"))))
(let [img (java.awt.image.BufferedImage. 48 48 java.awt.image.BufferedImage/TYPE_INT_ARGB) g (.createGraphics img)]
  (.scale g 2.4 2.4) (.invoke draw nil (object-array [g (tex "FIRST") (int 10) (int 10) (int 9) (st "FIRST") (boolean false)]))
  (javax.imageio.ImageIO/write img "png" (java.io.File. "C:/Users/hanjo/Claude/Projects/pid-indicator/icon.png")))
(println "ok")
