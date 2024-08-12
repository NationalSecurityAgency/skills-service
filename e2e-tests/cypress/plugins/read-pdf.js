const fs = require('fs')
const pdf = require('pdf-parse')

const readPdf = (filename) => {
  console.log('reading PDF file %s', filename)

  const dataBuffer = fs.readFileSync(filename)

  return pdf(dataBuffer).then(function (data) {
    console.log(data)
    return {
      numpages: data.numpages,
      text: data.text,
    }
  })
}

module.exports = { readPdf }