$ErrorActionPreference = 'Stop'
$p = 'c:\Users\lisihao\Desktop\李思豪-基于SpringBoot的智慧交通违章分析与预警系统的设计与实现-论文数据可视化数据可视化V1.docx'
if (-not (Test-Path -LiteralPath $p)) {
    Write-Output "FILE_NOT_FOUND: $p"
    exit 1
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($p)
try {
    $names = @($z.Entries | ForEach-Object { $_.FullName }) | Sort-Object
    Write-Output '--- ZIP ENTRIES (subset) ---'
    $names | Where-Object { $_ -match 'comment|Comment|revision|Revision|customXml' }
    $commentsEntry = $z.GetEntry('word/comments.xml')
    if ($null -eq $commentsEntry) {
        Write-Output 'NO word/comments.xml'
    } else {
        $sr = New-Object System.IO.StreamReader($commentsEntry.Open())
        try { $xml = $sr.ReadToEnd() } finally { $sr.Dispose() }
        Write-Output '--- RAW comments.xml (first 200KB) ---'
        $max = 200000
        if ($xml.Length -gt $max) { $xml.Substring(0, $max) + "`n...truncated..." } else { $xml }
    }
} finally {
    $z.Dispose()
}
