Add-Type -AssemblyName System.Drawing
foreach ($size in @(192, 512)) {
  $bitmap = New-Object System.Drawing.Bitmap($size, $size)
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#2563eb'))
  $brush = [System.Drawing.Brushes]::White
  $graphics.FillRectangle($brush, [int]($size * .425), [int]($size * .25), [int]($size * .15), [int]($size * .5))
  $graphics.FillRectangle($brush, [int]($size * .25), [int]($size * .425), [int]($size * .5), [int]($size * .15))
  $bitmap.Save((Join-Path $PSScriptRoot "../public/icon-$size.png"), [System.Drawing.Imaging.ImageFormat]::Png)
  $graphics.Dispose()
  $bitmap.Dispose()
}
