#!/bin/bash

# 1. Create the SSH run directory (Crucial for Ubuntu/Lubuntu)
mkdir -p /var/run/sshd
chmod 0755 /var/run/sshd

# 2. Create the user if it doesn't exist
if ! id "storage_user" &>/dev/null; then
    useradd -m -s /bin/bash storage_user
    echo "storage_user:storage_pass" | chpasswd
fi

# 3. Setup folders and permissions
mkdir -p /home/storage_user/uploads
chown -R storage_user:storage_user /home/storage_user

# 4. Generate SSH Host Keys (Sometimes missing in fresh containers)
ssh-keygen -A

# 5. Fix SSH Configuration
sed -i 's/^#\?PasswordAuthentication .*/PasswordAuthentication yes/' /etc/ssh/sshd_config
sed -i 's/^#\?PermitRootLogin .*/PermitRootLogin yes/' /etc/ssh/sshd_config

# 6. Start SSH in the FOREGROUND
echo "🚀 SSH starting on port 22..."
exec /usr/sbin/sshd -D -e