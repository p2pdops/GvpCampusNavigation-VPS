const fs = require('fs');
const data = require('./data.json');

const correctData = () => {
    const newData = data.map((item) => ({
        image: item.image,
        latitude: item.lat.length > 0 ? +item.lat[0] : null,
        longitude: item.long.length > 0 ? +item.long[0] : null,
    }));

    fs.writeFileSync('./corrected-data.json', JSON.stringify(newData, null, 2));
};

const createXlsx = (data = []) => {
    const XLSX = require('xlsx');
    const wb = XLSX.utils.book_new();
    const dataWithImages = data.map((item) => ({
        img: {
            t: 's',
            v: item.image,
            r: '<t>image1.jpg</t>',
            h: '<img src="image1.jpg" />',
            w: 'image1.jpg',

        }
        , ...item
    }));
    const ws = XLSX.utils.json_to_sheet(dataWithImages);


    dataWithImages.forEach((item, index) => {
        ws['!images'] = {
            name: item.image,
            data: fs.readFileSync(`./images/${item.image}`, 'base64'),
            opts: { base64: true },
            type: 'picture',
            filename: item.image,
            range: `A${index + 1}`,
        }
    });

    wb.Sheets['Sheet1'] = ws;

    // wb.Sheets[wb.SheetNames[1]]['!images'] = [
    //     {
    //         name: 'image1.jpg',
    //         type: 'picture',
    //         filename: 'image1.jpg',
    //         range: 'A1',
    //     },
    // ];


    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
    XLSX.writeFile(wb, 'data.xlsx');
}

createXlsx(require('./corrected-data.json'))