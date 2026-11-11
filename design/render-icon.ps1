# Render the same vector geometry as the Android adaptive icon, without external packages.
Add-Type -AssemblyName System.Drawing
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Write-Icon([int]$Pixels, [string]$Output, [bool]$Round) {
    $bitmap = [Drawing.Bitmap]::new($Pixels, $Pixels)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([Drawing.Color]::Transparent)
    $graphics.ScaleTransform(($Pixels / 108.0), ($Pixels / 108.0))
    $mask = [Drawing.Drawing2D.GraphicsPath]::new()
    if ($Round) { $mask.AddEllipse(0, 0, 108, 108) }
    else {
        $mask.AddArc(0, 0, 50, 50, 180, 90)
        $mask.AddArc(58, 0, 50, 50, 270, 90)
        $mask.AddArc(58, 58, 50, 50, 0, 90)
        $mask.AddArc(0, 58, 50, 50, 90, 90)
        $mask.CloseFigure()
    }
    $graphics.SetClip($mask)
    $gradient = [Drawing.Drawing2D.LinearGradientBrush]::new(
        [Drawing.Point]::new(0, 0), [Drawing.Point]::new(108, 108),
        [Drawing.ColorTranslator]::FromHtml('#285943'), [Drawing.ColorTranslator]::FromHtml('#102D24'))
    $graphics.FillRectangle($gradient, 0, 0, 108, 108)
    $orbit = [Drawing.Drawing2D.GraphicsPath]::new()
    $orbit.AddBezier(75, 63, 71, 75, 58, 82, 45, 77)
    $orbit.AddBezier(45, 77, 32, 73, 25, 60, 29, 47)
    $orbit.AddBezier(29, 47, 32, 36, 42, 28, 54, 28)
    $orbitPen = [Drawing.Pen]::new([Drawing.ColorTranslator]::FromHtml('#79B599'), 3.8)
    $orbitPen.StartCap = $orbitPen.EndCap = [Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawPath($orbitPen, $orbit)
    $check = [Drawing.Drawing2D.GraphicsPath]::new()
    $check.AddLine(39, 53, 50, 64)
    $check.AddLine(50, 64, 70, 42)
    $checkPen = [Drawing.Pen]::new([Drawing.ColorTranslator]::FromHtml('#DBF4BB'), 8.5)
    $checkPen.StartCap = $checkPen.EndCap = [Drawing.Drawing2D.LineCap]::Round
    $checkPen.LineJoin = [Drawing.Drawing2D.LineJoin]::Round
    $graphics.DrawPath($checkPen, $check)
    $dot = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#DBF4BB'))
    $graphics.FillEllipse($dot, 68, 28, 8, 8)
    $bitmap.Save($Output, [Drawing.Imaging.ImageFormat]::Png)
    $dot.Dispose(); $checkPen.Dispose(); $check.Dispose(); $orbitPen.Dispose(); $orbit.Dispose()
    $gradient.Dispose(); $mask.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
}

Write-Icon 1024 (Join-Path $PSScriptRoot 'app-icon.png') $false
Write-Icon 512 (Join-Path $PSScriptRoot 'app-icon-round.png') $true
$densities = @{mdpi=48; hdpi=72; xhdpi=96; xxhdpi=144; xxxhdpi=192}
foreach ($density in $densities.Keys) {
    $directory = Join-Path $projectRoot "app/src/main/res/mipmap-$density"
    Write-Icon $densities[$density] (Join-Path $directory 'ic_launcher.png') $false
    Write-Icon $densities[$density] (Join-Path $directory 'ic_launcher_round.png') $true
    # Remove only the superseded launcher images after both replacements exist.
    foreach ($name in @('ic_launcher.webp', 'ic_launcher_round.webp')) {
        $oldFile = Join-Path $directory $name
        if (Test-Path -LiteralPath $oldFile) { Remove-Item -LiteralPath $oldFile }
    }
}
