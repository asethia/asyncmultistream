# asyncmultistream
The provides few examples to demonstrate how we can read from multiple sources; where each source may have different SLA for source stream. Source retruns Future[List[T]], which is being transform into sequence of output elements using mapConcat.<br/><br/> 

The mapConcat transforms each input element into a sequence of output elements that is then flattened into the output stream. Sources are merged using merge junction (fan in) , which will pick randomly from inputs pushing them one by one to its output.<br/><br/>

The flow which is running parallel to save each element coming from upstream into database and respond back with Future. 

The tick package has another example; to demostrate how we can make regular (stream) source of ticks and store them into database.


