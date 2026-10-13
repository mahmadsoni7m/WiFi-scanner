# WiFi Scanner - оғози зуд (тоҷикӣ)

1. Дар GitHub репозиторийи нав созед (`+` > New repository > номи `WiFi-Scanner`).
2. Файли `WiFi-Scanner-Source.zip`-ро тавассути **Termux** бор кунед (папкаи `.github` ҳам нигоҳ дошта мешавад):
   ```bash
   pkg update && pkg install -y git unzip
   termux-setup-storage
   cd ~ && unzip ~/storage/downloads/WiFi-Scanner-Source.zip && cd WiFi-Scanner
   git init -b main && git add -A && git commit -m "Initial commit"
   git remote add origin https://github.com/НОМИ_ШУМО/WiFi-Scanner.git
   git push -u origin main
   ```
   Ба ҷои рамз «Personal Access Token» (иҷозатҳои `repo` ва `workflow`) истифода баред.
3. Дар GitHub: **Actions** > **Android Build** - сохтан худкор оғоз мешавад (ё **Run workflow**).
4. Пас аз тики сабз: run-ро кушоед > **Artifacts** > **WiFi-Scanner-debug-apk** > ZIP-ро кушоед ва `app-debug.apk`-ро насб кунед.
5. Агар сохтан хато диҳад, сатри аввали `error:`-ро аз қадами сурхшуда нусха кунед ва фиристед.

Диққат: муаллиф барномаро бе Android SDK навишт, бинобар ин он ҳанӯз тарҷума (compile) нашудааст; сохтани аввал дар GitHub Actions ҳақиқатан санҷида мешавад. Тафсилоти пурра: [README.md](README.md).
