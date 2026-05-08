$xml = @'
PLACEHOLDER
'@
[System.IO.File]::WriteAllText("c:\Users\Admin\Desktop\INTPROG\Hikora\app\src\main\res\layout\fragment_profile.xml", $xml, [System.Text.Encoding]::UTF8)
Write-Host "Done"