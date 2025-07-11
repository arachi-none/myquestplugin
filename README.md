# **MyQuest Plugin**

Selamat datang di plugin MyQuest\! Plugin ini memungkinkan Anda untuk membuat, mengelola, dan menyelesaikan misi di server Minecraft Anda.

## **1\. Membuat Misi Baru**

Untuk membuat misi baru, gunakan perintah dasar /myquest.

* **Perintah:** /myquest  
* **Fungsi:** Membuka GUI (Graphical User Interface) interaktif yang akan memandu Anda melalui proses pembuatan misi.  
  * Di GUI ini, Anda dapat mengatur judul misi, deskripsi, item yang dibutuhkan, dan item hadiah.  
  * Untuk item hadiah, Anda dapat menyeret dan meletakkan item langsung dari inventaris Anda ke dalam slot hadiah di GUI.

## **2\. Melihat Daftar Misi**

Anda dapat melihat semua misi yang telah dibuat di server.

* **Perintah:** /myquest list  
* **Fungsi:** Menampilkan daftar semua misi yang tersedia, beserta statusnya (Selesai atau Tertunda).  
  * Setiap entri misi akan memiliki tombol "Lihat Buku" yang dapat Anda klik untuk mendapatkan buku misi dari misi tersebut.

## **3\. Mendapatkan Buku Misi**

Buku misi adalah item yang akan diberikan kepada pemain lain agar mereka dapat menyelesaikan misi Anda.

* **Perintah:** /myquest getbook \<ID\_Misi\>  
* **Fungsi:** Memberikan Anda buku misi untuk misi dengan ID spesifik.  
  * **Catatan:** Anda biasanya akan mendapatkan buku misi secara otomatis setelah membuat misi baru. Perintah ini berguna jika Anda kehilangan buku misi atau ingin mendapatkan salinan tambahan.

## **4\. Menyelesaikan Misi**

Pemain yang ingin menyelesaikan misi harus memiliki buku misi dan semua item yang dibutuhkan di inventaris mereka.

* **Cara Menyelesaikan:**  
  1. Pegang buku misi yang relevan di tangan Anda.  
  2. Pastikan Anda memiliki semua item yang dibutuhkan di inventaris Anda.  
  3. Klik tombol **"\[Selesaikan Misi\]"** di dalam buku misi.  
* **Fungsi:**  
  * Jika semua persyaratan terpenuhi, item yang dibutuhkan akan diambil dari inventaris pemain.  
  * Pemain akan menerima item hadiah yang ditentukan.  
  * Misi akan ditandai sebagai selesai.  
  * Item yang dikumpulkan dari pemain yang menyelesaikan misi akan secara otomatis disimpan ke penyimpanan pembuat misi.

## **5\. Mengklaim Hasil Misi (untuk Pembuat Misi)**

Jika Anda adalah pembuat misi, item yang dikumpulkan oleh pemain yang menyelesaikan misi Anda akan disimpan untuk Anda. Anda bisa mengklaimnya kapan saja.

* **Perintah:** /myquest hasil  
* **Fungsi:** Membuka GUI klaim hasil misi.  
  * Di GUI ini, Anda akan melihat daftar semua item yang telah dikumpulkan dari misi Anda.  
  * Anda dapat mengklik item di GUI untuk memilihnya. Item yang dipilih akan memiliki efek *glow*.  
  * Setelah memilih item yang ingin Anda klaim, klik tombol **"Klaim Item Terpilih"** untuk memindahkannya ke inventaris Anda.  
  * Jika inventaris Anda penuh, item akan dijatuhkan di tanah di sekitar Anda.

## **Tips Tambahan**

* Pastikan Anda memiliki cukup ruang di inventaris Anda saat membuat misi (untuk buku misi) atau saat mengklaim hasil misi.  
* Jika Anda mengalami masalah, pastikan Anda menggunakan versi plugin dan server Minecraft yang kompatibel.

## **Catatan**
* Masih dalam development, mohon maaf jika terdapat beberapa bug.
* Untuk saat ini tidak tersedia untuk membuat quest mencari potion, namun dapat memberikan reward berupa potion.
* Quest hanya berlaku sekali, jika telah diselesaikan orang lain maka tidak akan dapat di Selesaikan.
* Dapat mengambil quest melalui /myquest list -> cari yang masih pending -> lihat quest.

  Dikerjakan Penuh cinta oleh Arachi :)
