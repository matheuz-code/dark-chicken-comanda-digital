-- RF-001: os pratos de exemplo do cardápio público devem possuir foto.
UPDATE prato
SET foto_url = 'https://images.unsplash.com/photo-1561758033-d89a9ad46330?auto=format&fit=crop&w=900&q=80'
WHERE id = 1 AND foto_url IS NULL;

UPDATE prato
SET foto_url = 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80'
WHERE id = 2 AND foto_url IS NULL;

UPDATE prato
SET foto_url = 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=900&q=80'
WHERE id = 3 AND foto_url IS NULL;

UPDATE prato
SET foto_url = 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?auto=format&fit=crop&w=900&q=80'
WHERE id = 4 AND foto_url IS NULL;

UPDATE prato
SET foto_url = 'https://wallpapers.com/images/hd/coca-cola-931-x-1392-picture-27hdq23tvf06e5q4.jpg'
WHERE id = 5 AND foto_url IS NULL;
