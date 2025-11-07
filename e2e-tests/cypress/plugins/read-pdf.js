const fs = require('fs')
const { PDFParse } = require('pdf-parse');

const readPdf = (filename) => {
  const dataBuffer = fs.readFileSync(filename)
  const parser = new PDFParse({ data: dataBuffer });

  return parser.getText().then(function (data) {
      return {
          numpages: data.pages?.length || -1,
          text: data.text,
    }
  })
}

module.exports = { readPdf }