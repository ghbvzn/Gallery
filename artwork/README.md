# Gallery launcher icon

The new launcher artwork is `gallery-icon-source.png`, generated using the built-in image generation tool. `gallery-icon-preview.png` shows circular and rounded-square launcher masks plus the themed monochrome icon.

The foreground is packaged in `app/src/main/res/drawable-nodpi/gallery_icon_foreground.png`. Android's adaptive icon uses a 52 dp foreground inside the 108 dp canvas to keep the artwork within mask-safe bounds. Legacy launcher icons are exported at all five Android densities. The monochrome companion is a native Android vector.

Generation prompt:

> Use case: logo-brand. Asset type: production Android gallery launcher foreground emblem, square high resolution canvas with genuine transparent background. Create one original polished gallery symbol: two lightly overlapping rounded photo frames, the foremost containing a bold simplified mountain silhouette and a small warm sun. Use the existing app's lavender/lilac and warm rose accents with bright ivory framing, restrained smooth gradients and subtle layered depth. This should read instantly as photos and memories at tiny launcher size, feel elegant and modern, with a clean substantial silhouette and crisp edges. Center one complete emblem on transparent canvas with generous clear padding, front-facing. No text, letters, logos of other apps, watermarks, surrounding app tile, checkerboard, mockup, extra decorative objects or scene background.

The original generated alpha is preserved. Density export and mask previews use deterministic image resizing and compositing; the foreground itself is not redrawn.
